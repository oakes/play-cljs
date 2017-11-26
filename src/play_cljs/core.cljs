(ns play-cljs.core
  (:require [goog.events :as events]
            [p5.core]
            [p5.tiled-map]
            [cljs.core.async :refer [promise-chan put! <!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

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
  (on-show [this]
    "Runs once, when the screen first appears.")
  (on-hide [this]
    "Runs once, when the screen is no longer displayed.")
  (on-render [this]
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

;(set! *warn-on-infer* true)

(set! (.-disableFriendlyErrors ^js/p5 js/p5) true)

(defn update-opts [opts parent-opts defaults]
  (let [parent-opts (merge defaults parent-opts)]
    (-> (merge defaults (dissoc parent-opts :x :y) opts)
        (update :x + (:x parent-opts))
        (update :y + (:y parent-opts)))))

(def ^:const basic-defaults {:x 0 :y 0 :scale-x 1 :scale-y 1})
(def ^:const text-defaults (merge basic-defaults {:size 32 :font "Helvetica" :halign :left :valign :baseline :leading 0 :style :normal}))
(def ^:const img-defaults (merge basic-defaults {:sx 0 :sy 0}))
(def ^:const rgb-defaults (merge basic-defaults {:max-r 255 :max-g 255 :max-b 255 :max-a 1}))
(def ^:const hsb-defaults (merge basic-defaults {:max-h 360 :max-s 100 :max-b 100 :max-a 1}))

(defn halign->constant [^js/p5 renderer halign]
  (get {:left (.-LEFT renderer) :center (.-CENTER renderer) :right (.-RIGHT renderer)} halign))

(defn valign->constant [^js/p5 renderer valign]
  (get {:top (.-TOP renderer) :center (.-CENTER renderer) :bottom (.-BOTTOM renderer) :baseline (.-BASELINE renderer)} valign))

(defn style->constant [^js/p5 renderer style]
  (get {:normal (.-NORMAL renderer) :italic (.-ITALIC renderer) :bold (.-BOLD renderer)} style))

(defmulti draw-sketch! (fn [game ^js/p5 renderer content parent-opts]
                         (first content)))

(defmethod draw-sketch! :div [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        opts (update-opts opts parent-opts basic-defaults)]
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :text [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [value x y size font halign valign leading style] :as opts}
        (update-opts opts parent-opts text-defaults)]
    (doto renderer
      (.textSize size)
      (.textFont font)
      (.textAlign (halign->constant renderer halign) (valign->constant renderer valign))
      (.textLeading leading)
      (.textStyle (style->constant renderer style))
      (.text value x y))
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :arc [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [x y width height start stop] :as opts}
        (update-opts opts parent-opts basic-defaults)]
    (.arc renderer x y width height start stop)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :ellipse [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [x y width height] :as opts}
        (update-opts opts parent-opts basic-defaults)]
    (.ellipse renderer x y width height)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :line [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        opts (update-opts opts parent-opts basic-defaults)
        {:keys [x1 y1 x2 y2] :as opts}
        (-> opts
            (update :x1 + (:x opts))
            (update :y1 + (:y opts))
            (update :x2 + (:x opts))
            (update :y2 + (:y opts)))]
    (.line renderer x1 y1 x2 y2)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :point [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [x y] :as opts}
        (update-opts opts parent-opts basic-defaults)]
    (.point renderer x y)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :quad [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        opts (update-opts opts parent-opts basic-defaults)
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
  (let [[command opts & children] content
        {:keys [x y width height] :as opts}
        (update-opts opts parent-opts basic-defaults)]
    (.rect renderer x y width height)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :triangle [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        opts (update-opts opts parent-opts basic-defaults)
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
  (let [[command opts & children] content
        {:keys [value name x y width height sx sy swidth sheight scale-x scale-y flip-x flip-y]
         :as opts} (update-opts opts parent-opts img-defaults)
        ^js/p5.Image value (or value
                               (get-asset game name)
                               (load-image game name))
        swidth (or swidth (.-width value))
        sheight (or sheight (.-height value))]
    (.push renderer)
    (.translate renderer x y)
    (.scale renderer scale-x scale-y)
    (when flip-x
      (.scale renderer -1 1)
      (.translate renderer (- swidth) 0))
    (when flip-y
      (.scale renderer 1 -1)
      (.translate renderer 0 (- sheight)))
    (.image renderer value
      0 0 (or width swidth) (or height sheight)
      sx sy swidth sheight)
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defmethod draw-sketch! :animation [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [duration] :as opts} (update-opts opts parent-opts basic-defaults)
        images (vec children)
        cycle-time (mod (get-total-time game) (* duration (count images)))
        index (int (/ cycle-time duration))
        image (get images index)]
    (draw-sketch! game renderer image opts)))

(defmethod draw-sketch! :fill [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [grayscale color colors] :as opts}
        (update-opts opts parent-opts basic-defaults)]
    (.push renderer)
    (cond
      grayscale
      (.fill renderer grayscale)
      color
      (.fill renderer color)
      colors
      (let [[n1 n2 n3] colors]
        (.fill renderer n1 n2 n3))
      :else
      (.noFill renderer))
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defmethod draw-sketch! :stroke [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [grayscale color colors] :as opts}
        (update-opts opts parent-opts basic-defaults)]
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
  (let [[command opts & children] content
        opts (update-opts opts parent-opts basic-defaults)
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
    (cond
      (and x1 y1 x2 y2 x3 y3 x4 y4)
      (.bezier renderer x1 y1 x2 y2 x3 y3 x4 y4)
      (and z1 z2 z3 z4)
      (.bezier renderer z1 z2 z3 z4)
      :else
      (throw "Invalid args for bezier"))
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :curve [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        opts (update-opts opts parent-opts basic-defaults)
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
    (cond
      (and x1 y1 x2 y2 x3 y3 x4 y4)
      (.curve renderer x1 y1 x2 y2 x3 y3 x4 y4)
      (and z1 z2 z3 z4)
      (.curve renderer z1 z2 z3 z4)
      :else
      (throw "Invalid args for curve"))
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :rgb [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [max-r max-g max-b max-a] :as opts}
        (update-opts opts parent-opts rgb-defaults)]
    (.push renderer)
    (.colorMode renderer (.-RGB renderer) max-r max-g max-b max-a)
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defmethod draw-sketch! :hsb [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [max-h max-s max-b max-a] :as opts}
        (update-opts opts parent-opts hsb-defaults)]
    (.push renderer)
    (.colorMode renderer (.-HSB renderer) max-h max-s max-b max-a)
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defmethod draw-sketch! :tiled-map [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [value name x y] :as opts}
        (update-opts opts parent-opts basic-defaults)
        ^js/p5.TiledMap value (or value
                                  (get-asset game name)
                                  (load-tiled-map game name))]
    (.draw value x y)
    (draw-sketch! game renderer children opts)))

(defmethod draw-sketch! :shape [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        opts (update-opts opts parent-opts basic-defaults)
        points (:points opts)]
    (cond (odd? (count points))
          (throw ":shape requires :points to contain a seq'able with an even number of values (x and y pairs)")
          :else
          (do (.beginShape renderer)
              (loop [[x y & rest] points]
                    (.vertex renderer x y)
                    (when rest
                      (recur rest)))
              (draw-sketch! game renderer children opts)
              (.endShape renderer (.-CLOSE renderer))))))

(defmethod draw-sketch! :contour [game ^js/p5 renderer content parent-opts]
  (let [[command opts & children] content
        opts (update-opts opts parent-opts basic-defaults)
        points (:points opts)]
    (cond (odd? (count points))
          (throw ":contour requires :points to contain a seq'able with an even number of values (x and y pairs)")
          :else
          (do (.beginContour renderer)
              (loop [[x y & rest] points]
                    (.vertex renderer x y)
                    (when rest
                      (recur rest)))
              (draw-sketch! game renderer children opts)
              (.endContour renderer (.-CLOSE renderer))))))

(defmethod draw-sketch! :default [game ^js/p5 renderer content parent-opts]
  (cond
    (sequential? (first content))
    (run! #(draw-sketch! game renderer % parent-opts) content)
    (nil? (first content))
    nil
    :else
    (throw (js/Error. (str "Invalid sketch command: " (pr-str content))))))

(defn create-game
  "Returns a game object."
  ([width height]
   (create-game width height {}))
  ([width height {:keys [parent]}]
   (let [hidden-state-atom (atom {:screen nil
                                  :renderer nil
                                  :canvas nil
                                  :listeners []
                                  :total-time 0
                                  :delta-time 0
                                  :pressed-keys #{}
                                  :assets {}})
         setup-finished? (promise-chan)]
     (reify Game
       (start [this]
         (when-let [^js/p5 renderer (get-renderer this)]
           (.remove renderer))
         (run! events/unlistenByKey (:listeners @hidden-state-atom))
         (swap! hidden-state-atom assoc :listeners [])
         (js/p5.
           (fn [^js/p5 renderer]
             (set! (.-setup renderer)
               (fn []
                 ;; create the canvas
                 (let [^js/p5 canvas-wrapper (cond-> (.createCanvas renderer width height)
                                               parent (.parent parent))
                       canvas (.-canvas canvas-wrapper)]
                   (.removeAttribute canvas "style")
                   (swap! hidden-state-atom assoc :renderer renderer :canvas canvas))
                 ;; allow on-show to be run
                 (put! setup-finished? true)))
             ;; set the draw function
             (set! (.-draw renderer)
               (fn []
                 (swap! hidden-state-atom
                   (fn [hidden-state]
                     (let [time (.millis renderer)]
                       (assoc hidden-state
                         :total-time time
                         :delta-time (- time (:total-time hidden-state))))))
                 (.clear renderer)
                 (some-> this get-screen on-render)))))
         (listen this "keydown"
            (fn [^js/KeyboardEvent e]
              (swap! hidden-state-atom update :pressed-keys conj (.-keyCode e))))
         (listen this "keyup"
           (fn [^js/KeyboardEvent e]
             (if (contains? #{91 93} (.-keyCode e))
               (swap! hidden-state-atom assoc :pressed-keys #{})
               (swap! hidden-state-atom update :pressed-keys disj (.-keyCode e)))))
         (listen this "blur"
           #(swap! hidden-state-atom assoc :pressed-keys #{})))
       (listen [this listen-type listener]
         (swap! hidden-state-atom update :listeners conj
                (events/listen js/window listen-type listener)))
       (render [this content]
         (when-let [^js/p5 renderer (get-renderer this)]
           (draw-sketch! this renderer content {})))
       (pre-render [this image-name width height content]
         (when-let [^js/p5 renderer (get-renderer this)]
           (let [object (.createGraphics renderer width height)]
             (draw-sketch! this object content {})
             (swap! hidden-state-atom update :assets assoc image-name object)
             object)))
       (load-image [this path]
         (when-let [^js/p5 renderer (get-renderer this)]
           (let [object (.loadImage renderer path (fn []))]
             (swap! hidden-state-atom update :assets assoc path object)
             object)))
       (load-tiled-map [this map-name]
         (when-let [^js/p5 renderer (get-renderer this)]
           (let [object (.loadTiledMap renderer map-name (fn []))]
             (swap! hidden-state-atom update :assets assoc map-name object)
             object)))
       (get-screen [this]
         (:screen @hidden-state-atom))
       (set-screen [this screen]
         (go
           ;; wait for the setup function to finish
           (<! setup-finished?)
           ;; change the screens
           (some-> this get-screen on-hide)
           (swap! hidden-state-atom assoc :screen screen)
           (on-show screen)))
       (get-renderer [this]
         (:renderer @hidden-state-atom))
       (get-canvas [this]
         (:canvas @hidden-state-atom))
       (get-total-time [this]
         (:total-time @hidden-state-atom))
       (get-delta-time [this]
         (:delta-time @hidden-state-atom))
       (get-pressed-keys [this]
         (:pressed-keys @hidden-state-atom))
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
         (get-in @hidden-state-atom [:assets name]))))))
