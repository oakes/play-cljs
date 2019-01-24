(ns play-cljs.core
  (:require [goog.events :as events]
            [cljsjs.p5]
            [p5.tiled-map]
            [cljs.core.async :refer [promise-chan put! <!]]
            [play-cljs.examples]
            [play-cljs.options :as options])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; protocols and multimethods

(defprotocol Screen
  "A screen object provides the basic lifecycle for a game.
Simple games may only need to have one screen. They are a useful way to
isolate different aspects of your game. For example, you could make one
screen display the title and menu, and another screen contain the game
itself.

You can create a screen by using `reify` like this:

```
(def main-screen
  (reify p/Screen
    (on-show [this])
    (on-hide [this])
    (on-render [this])))
```"
  (on-show [screen]
    "Runs once, when the screen first appears.")
  (on-hide [screen]
    "Runs once, when the screen is no longer displayed.")
  (on-render [screen]
    "Runs each time the game is ready to render another frame."))

(defprotocol Game
  "A game object contains the internal renderer object and various bits of state
that are important to the overall execution of the game. Every play-cljs game
should create just one such object by calling `create-game`."
  (start [game]
    "Creates the canvas element.")
  (listen [game listen-type listener]
    "Adds an event listener.")
  (render [game content]
    "Renders the provided data structure.")
  (pre-render [game image-name width height content]
    "Renders the provided data structure off-screen and associates it with the given name. Returns an `Image` object.")
  (load-image [game path]
    "Loads an image. Returns an `Image` object.")
  (load-tiled-map [game map-name]
    "Loads a tiled map. Returns a `TiledMap` object.
A tiled map with the provided name must already be loaded
(see the TiledMap docs for details).")
  (get-screen [game]
    "Returns the `Screen` object currently being displayed.")
  (set-screen [game screen]
    "Sets the `Screen` object to be displayed.")
  (get-renderer [game]
    "Returns the internal renderer object.")
  (get-canvas [game]
    "Returns the internal canvas object.")
  (get-total-time [game]
    "Returns the total time transpired since the game started, in milliseconds.")
  (get-delta-time [game]
    "Returns the time since the last frame was rendered, in milliseconds.")
  (get-pressed-keys [game]
    "Returns a set containing the key codes for the keys currently being pressed.")
  (get-width [game]
    "Returns the virtual width of the game.")
  (get-height [game]
    "Returns the virtual height of the game.")
  (set-size [game width height]
    "Sets the virtual width and height of the game.")
  (get-asset [game name]
    "Gets the asset with the given name."))

(defmulti draw-sketch!
  "Internal multimethod for drawing entities. Extending this will allow you
to define new entity types."
  (fn [game renderer content parent-opts]
    (let [k (first content)]
      (cond
        (keyword? k) k
        (sequential? k) ::multiple))))

(defmethod draw-sketch! ::multiple [game ^js/p5 renderer content parent-opts]
  (run! #(draw-sketch! game renderer % parent-opts) content))

(defmethod draw-sketch! :default [game ^js/p5 renderer content parent-opts]
  (when-let [name (first content)]
    (throw (js/Error. (str "Command not found: " name)))))

(defmethod draw-sketch! :div [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/basic-defaults)]
    (when (:debug? opts) (options/check-opts ::options/basic-opts opts))
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :translate [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/basic-defaults)
        {:keys [x y z]} opts]
    (when (:debug? opts) (options/check-opts ::options/translate-opts opts))
    (.push renderer)
    (if z
      (.translate renderer x y z)
      (.translate renderer x y))
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defmethod draw-sketch! :rotate [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [angle axis]} opts]
    (when (:debug? opts) (options/check-opts ::options/rotate-opts opts))
    (.push renderer)
    (case axis
      :x (.rotateX renderer angle)
      :y (.rotateY renderer angle)
      :z (.rotateZ renderer angle)
      (.rotate renderer angle))
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

;; 2d

(defmethod draw-sketch! :text [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [value x y size font halign valign leading style] :as opts}
        (options/update-opts opts parent-opts options/text-defaults)]
    (when (:debug? opts) (options/check-opts ::options/text-opts opts))
    (doto renderer
      (.textSize size)
      (.textFont font)
      (.textAlign (options/halign->constant renderer halign) (options/valign->constant renderer valign))
      (.textLeading leading)
      (.textStyle (options/style->constant renderer style))
      (.text value x y))
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :arc [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [x y width height start stop] :as opts}
        (options/update-opts opts parent-opts options/basic-defaults)]
    (when (:debug? opts) (options/check-opts ::options/arc-opts opts))
    (.arc renderer x y width height start stop)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :ellipse [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [x y width height] :as opts}
        (options/update-opts opts parent-opts options/basic-defaults)]
    (when (:debug? opts) (options/check-opts ::options/ellipse-opts opts))
    (.ellipse renderer x y width height)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :line [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/basic-defaults)
        _ (when (:debug? opts) (options/check-opts ::options/line-opts opts))
        {:keys [x1 y1 x2 y2] :as opts}
        (-> opts
            (update :x1 + (:x opts))
            (update :y1 + (:y opts))
            (update :x2 + (:x opts))
            (update :y2 + (:y opts)))]
    (.line renderer x1 y1 x2 y2)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :point [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [x y] :as opts}
        (options/update-opts opts parent-opts options/basic-defaults)]
    (when (:debug? opts) (options/check-opts ::options/point-opts opts))
    (.point renderer x y)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :quad [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/basic-defaults)
        _ (when (:debug? opts) (options/check-opts ::options/quad-opts opts))
        {:keys [x1 y1 x2 y2 x3 y3 x4 y4] :as opts}
        (-> opts
            (update :x1 + (:x opts))
            (update :y1 + (:y opts))
            (update :x2 + (:x opts))
            (update :y2 + (:y opts))
            (update :x3 + (:x opts))
            (update :y3 + (:y opts))
            (update :x4 + (:x opts))
            (update :y4 + (:y opts)))]
    (.quad renderer x1 y1 x2 y2 x3 y3 x4 y4)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :rect [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [x y width height] :as opts}
        (options/update-opts opts parent-opts options/basic-defaults)]
    (when (:debug? opts) (options/check-opts ::options/rect-opts opts))
    (.rect renderer x y width height)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :triangle [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/basic-defaults)
        _ (when (:debug? opts) (options/check-opts ::options/triangle-opts opts))
        {:keys [x1 y1 x2 y2 x3 y3] :as opts}
        (-> opts
            (update :x1 + (:x opts))
            (update :y1 + (:y opts))
            (update :x2 + (:x opts))
            (update :y2 + (:y opts))
            (update :x3 + (:x opts))
            (update :y3 + (:y opts)))]
    (.triangle renderer x1 y1 x2 y2 x3 y3)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :image [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [value name x y width height sx sy swidth sheight scale-x scale-y flip-x flip-y flip-x? flip-y?]
         :as opts} (options/update-opts opts parent-opts options/image-defaults)
        _ (when (:debug? opts) (options/check-opts ::options/image-opts opts))
        ^js/p5.Image value (or value
                               (get-asset game name)
                               (load-image game name))
        swidth (or swidth (.-width value))
        sheight (or sheight (.-height value))]
    (.push renderer)
    (.translate renderer x y)
    (.scale renderer scale-x scale-y)
    (when (or flip-x? flip-x)
      (.scale renderer -1 1)
      (.translate renderer (- swidth) 0))
    (when (or flip-y? flip-y)
      (.scale renderer 1 -1)
      (.translate renderer 0 (- sheight)))
    (.image renderer value
      0 0 (or width swidth) (or height sheight)
      sx sy swidth sheight)
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defmethod draw-sketch! :animation [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [duration] :as opts} (options/update-opts opts parent-opts options/basic-defaults)
        _ (when (:debug? opts) (options/check-opts ::options/animation-opts opts))
        images (vec children)
        cycle-time (mod (get-total-time game) (* duration (count images)))
        index (int (/ cycle-time duration))
        image (get images index)]
    (draw-sketch! game renderer image opts)))

(defmethod draw-sketch! :fill [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [grayscale color colors] :as opts}
        (options/update-opts opts parent-opts options/basic-defaults)]
    (when (:debug? opts) (options/check-opts ::options/fill-opts opts))
    (.push renderer)
    (cond
      grayscale
      (.fill renderer grayscale)
      color
      (.fill renderer color)
      colors
      (let [[n1 n2 n3 a] colors]
        (.fill renderer n1 n2 n3 a))
      :else
      (.noFill renderer))
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defmethod draw-sketch! :stroke [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [grayscale color colors] :as opts}
        (options/update-opts opts parent-opts options/basic-defaults)]
    (when (:debug? opts) (options/check-opts ::options/stroke-opts opts))
    (.push renderer)
    (cond
      grayscale
      (.stroke renderer grayscale)
      color
      (.stroke renderer color)
      colors
      (let [[n1 n2 n3] colors]
        (.stroke renderer n1 n2 n3))
      :else
      (.noStroke renderer))
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defmethod draw-sketch! :bezier [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/basic-defaults)
        _ (when (:debug? opts) (options/check-opts ::options/bezier-opts opts))
        {:keys [x1 y1 x2 y2 x3 y3 x4 y4
                z1 z2 z3 z4] :as opts}
        (-> opts
            (update :x1 + (:x opts))
            (update :y1 + (:y opts))
            (update :x2 + (:x opts))
            (update :y2 + (:y opts))
            (update :x3 + (:x opts))
            (update :y3 + (:y opts))
            (update :x4 + (:x opts))
            (update :y4 + (:y opts)))]
    (if (and z1 z2 z3 z4)
      (.bezier renderer x1 y1 z1 x2 y2 z2 x3 y3 z3 x4 y4 z4)
      (.bezier renderer x1 y1 x2 y2 x3 y3 x4 y4))
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :curve [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/basic-defaults)
        _ (when (:debug? opts) (options/check-opts ::options/curve-opts opts))
        {:keys [x1 y1 x2 y2 x3 y3 x4 y4
                z1 z2 z3 z4] :as opts}
        (-> opts
            (update :x1 + (:x opts))
            (update :y1 + (:y opts))
            (update :x2 + (:x opts))
            (update :y2 + (:y opts))
            (update :x3 + (:x opts))
            (update :y3 + (:y opts))
            (update :x4 + (:x opts))
            (update :y4 + (:y opts)))]
    (if (and z1 z2 z3 z4)
      (.curve renderer x1 y1 z1 x2 y2 z2 x3 y3 z3 x4 y4 z4)
      (.curve renderer x1 y1 x2 y2 x3 y3 x4 y4))
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :rgb [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [max-r max-g max-b max-a] :as opts}
        (options/update-opts opts parent-opts options/basic-defaults)]
    (when (:debug? opts) (options/check-opts ::options/rgb-opts opts))
    (.push renderer)
    (.colorMode renderer (.-RGB renderer) max-r max-g max-b max-a)
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defmethod draw-sketch! :hsb [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [max-h max-s max-b max-a] :as opts}
        (options/update-opts opts parent-opts options/basic-defaults)]
    (when (:debug? opts) (options/check-opts ::options/hsb-opts opts))
    (.push renderer)
    (.colorMode renderer (.-HSB renderer) max-h max-s max-b max-a)
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defmethod draw-sketch! :hsl [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [max-h max-s max-l max-a] :as opts}
        (options/update-opts opts parent-opts options/basic-defaults)]
    (when (:debug? opts) (options/check-opts ::options/hsl-opts opts))
    (.push renderer)
    (.colorMode renderer (.-HSL renderer) max-h max-s max-l max-a)
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defmethod draw-sketch! :tiled-map [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        {:keys [value name x y] :as opts}
        (options/update-opts opts parent-opts options/basic-defaults)
        _ (when (:debug? opts) (options/check-opts ::options/tiled-map-opts opts))
        ^js/p5.TiledMap value (or value
                                  (get-asset game name)
                                  (load-tiled-map game name))]
    (.draw value x y)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :shape [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/basic-defaults)]
    (when (:debug? opts) (options/check-opts ::options/shape-opts opts))
    (.beginShape renderer)
    (loop [[x y & rest] (:points opts)]
      (.vertex renderer x y)
      (when rest
        (recur rest)))
    (draw-sketch! game renderer children opts)
    (.endShape renderer (.-CLOSE renderer))))

(defmethod draw-sketch! :contour [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/basic-defaults)]
    (when (:debug? opts) (options/check-opts ::options/contour-opts opts))
    (.beginContour renderer)
    (loop [[x y & rest] (:points opts)]
      (.vertex renderer x y)
      (when rest
        (recur rest)))
    (draw-sketch! game renderer children opts)
    (.endContour renderer (.-CLOSE renderer))))

;; 3d

(defmethod draw-sketch! :plane [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/plane-defaults)
        {:keys [width height detail-x detail-y]} opts]
    (when (:debug? opts) (options/check-opts ::options/plane-opts opts))
    (.plane renderer width height detail-x detail-y)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :box [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/box-defaults)
        {:keys [width height depth detail-x detail-y]} opts]
    (when (:debug? opts) (options/check-opts ::options/box-opts opts))
    (.box renderer width height depth detail-x detail-y)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :sphere [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/sphere-defaults)
        {:keys [radius detail-x detail-y]} opts]
    (when (:debug? opts) (options/check-opts ::options/sphere-opts opts))
    (.sphere renderer radius detail-x detail-y)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :cylinder [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/cylinder-defaults)
        {:keys [radius height detail-x detail-y bottom-cap? top-cap?]} opts]
    (when (:debug? opts) (options/check-opts ::options/cylinder-opts opts))
    (.cylinder renderer radius height detail-x detail-y bottom-cap? top-cap?)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :cone [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/cone-defaults)
        {:keys [radius height detail-x detail-y cap?]} opts]
    (when (:debug? opts) (options/check-opts ::options/cone-opts opts))
    (.cone renderer radius height detail-x detail-y cap?)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :ellipsoid [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/ellipsoid-defaults)
        {:keys [radius-x radius-y radius-z detail-x detail-y]} opts]
    (when (:debug? opts) (options/check-opts ::options/ellipsoid-opts opts))
    (.ellipsoid renderer radius-x radius-y radius-z detail-x detail-y)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :torus [game ^js/p5 renderer content parent-opts]
  (let [[_ opts & children] content
        opts (options/update-opts opts parent-opts options/torus-defaults)
        {:keys [radius tube-radius detail-x detail-y]} opts]
    (when (:debug? opts) (options/check-opts ::options/torus-opts opts))
    (.torus renderer radius tube-radius detail-x detail-y)
    (draw-sketch! game renderer children opts)))

;; creating a game

(defn ^:private start-example-game [game card *state]
  (doto game
    (start)
    (set-size (.-clientWidth card) (.-clientHeight card))
    (listen "mousemove"
      (fn [event]
        (let [bounds (.getBoundingClientRect card)
              x (- (.-clientX event) (.-left bounds))
              y (- (.-clientY event) (.-top bounds))]
          (swap! *state assoc :x x :y y))))
    (listen "resize"
      (fn [event]
        (set-size game (.-clientWidth card) (.-clientHeight card))))))

(defn create-game
  "Returns a game object. You can pass an options map with the following:
  
  :parent  -  A DOM element in which to place the canvas
  :debug?  -  Whether or not to enable debug mode
              (defaults to true if :optimizations are set to :none)
  :mode    -  Either :2d or :webgl (defaults to :2d)"
  ([width height]
   (create-game width height {}))
  ([width height {:keys [parent debug? mode]
                  :or {debug? (not js/COMPILED)
                       mode :2d}
                  :as opts}]
   (if debug?
     (js/console.log
       (str "Debug mode is enabled. If things are slow, try passing "
         "{:debug? false} to the third argument of create-game."))
     (set! (.-disableFriendlyErrors ^js/p5 js/p5) true))
   (let [*hidden-state (atom {:screen nil
                              :renderer nil
                              :canvas nil
                              :listeners []
                              :total-time 0
                              :delta-time 0
                              :pressed-keys #{}
                              :assets {}})
         setup-finished? (promise-chan)
         parent-opts (if debug? {:debug? true} {})]
     (reify Game
       (start [this]
         (when-let [^js/p5 renderer (get-renderer this)]
           (.remove renderer))
         (run! events/unlistenByKey (:listeners @*hidden-state))
         (swap! *hidden-state assoc :listeners [])
         (js/p5.
           (fn [^js/p5 renderer]
             (set! (.-setup renderer)
               (fn []
                 ;; create the canvas
                 (let [^js/p5 canvas-wrapper (cond-> (.createCanvas renderer width height
                                                       (case mode
                                                         :2d (.-P2D renderer)
                                                         :webgl (.-WEBGL renderer)))
                                                     parent (.parent parent))
                       canvas (.-canvas canvas-wrapper)]
                   (.removeAttribute canvas "style")
                   (swap! *hidden-state assoc :renderer renderer :canvas canvas))
                 ;; allow on-show to be run
                 (put! setup-finished? true)))
             ;; set the draw function
             (set! (.-draw renderer)
               (fn []
                 (swap! *hidden-state
                   (fn [hidden-state]
                     (let [time (.millis renderer)]
                       (assoc hidden-state
                         :total-time time
                         :delta-time (- time (:total-time hidden-state))))))
                 (.clear renderer)
                 (some-> this get-screen on-render)))))
         (listen this "keydown"
           (fn [^js/KeyboardEvent e]
             (swap! *hidden-state update :pressed-keys conj (.-keyCode e))))
         (listen this "keyup"
           (fn [^js/KeyboardEvent e]
             (if (contains? #{91 93} (.-keyCode e))
               (swap! *hidden-state assoc :pressed-keys #{})
               (swap! *hidden-state update :pressed-keys disj (.-keyCode e)))))
         (listen this "blur"
           #(swap! *hidden-state assoc :pressed-keys #{})))
       (listen [this listen-type listener]
         (swap! *hidden-state update :listeners conj
           (events/listen js/window listen-type listener)))
       (render [this content]
         (when-let [^js/p5 renderer (get-renderer this)]
           (draw-sketch! this renderer content parent-opts)))
       (pre-render [this image-name width height content]
         (when-let [^js/p5 renderer (get-renderer this)]
           (let [object (.createGraphics renderer width height)]
             (draw-sketch! this object content parent-opts)
             (swap! *hidden-state update :assets assoc image-name object)
             object)))
       (load-image [this path]
         (when-let [^js/p5 renderer (get-renderer this)]
           (let [object (.loadImage renderer path (fn []))]
             (swap! *hidden-state update :assets assoc path object)
             object)))
       (load-tiled-map [this map-name]
         (when-let [^js/p5 renderer (get-renderer this)]
           (let [object (.loadTiledMap renderer map-name (fn []))]
             (swap! *hidden-state update :assets assoc map-name object)
             object)))
       (get-screen [this]
         (:screen @*hidden-state))
       (set-screen [this screen]
         (go
           ;; wait for the setup function to finish
           (<! setup-finished?)
           ;; change the screens
           (some-> this get-screen on-hide)
           (swap! *hidden-state assoc :screen screen)
           (on-show screen)))
       (get-renderer [this]
         (:renderer @*hidden-state))
       (get-canvas [this]
         (:canvas @*hidden-state))
       (get-total-time [this]
         (:total-time @*hidden-state))
       (get-delta-time [this]
         (:delta-time @*hidden-state))
       (get-pressed-keys [this]
         (:pressed-keys @*hidden-state))
       (get-width [this]
         (when-let [^js/p5 renderer (get-renderer this)]
           (.-width renderer)))
       (get-height [this]
         (when-let [^js/p5 renderer (get-renderer this)]
           (.-height renderer)))
       (set-size [this width height]
         (when-let [^js/p5 renderer (get-renderer this)]
           (.resizeCanvas renderer width height)))
       (get-asset [game name]
         (get-in @*hidden-state [:assets name]))))))

