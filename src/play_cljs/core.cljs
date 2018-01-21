(ns play-cljs.core
  (:require [goog.events :as events]
            [p5.core]
            [p5.tiled-map]
            [cljs.core.async :refer [promise-chan put! <!]]
            [play-cljs.utils :as utils]
            [clojure.spec.alpha :as s])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [dynadoc.example :refer [defexample]]))

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
    "Gets the asset with the given name.")
  (get-option [game k]
    "Gets the option associated with k."))

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

(defexample draw-sketch!
  {:doc "Creates a new entity type called :smiley that draws a smiley face.
After defining the method, it can be rendered like this: [:smiley {:x 0 :y 0}]"
   :with-card card
   :with-callback callback
   :with-focus [focus (defmethod play-cljs.core/draw-sketch! :smiley [game ^js/p5 renderer content parent-opts]
                        (let [[_ opts & children] content
                              opts (play-cljs.utils/update-opts opts parent-opts play-cljs.utils/basic-defaults)]
                          (play-cljs.core/draw-sketch!
                            game
                            renderer
                            [:div {:x 100 :y 100}
                             [:fill {:color "yellow"}
                              [:ellipse {:width 100 :height 100}
                               [:fill {:color "black"}
                                [:ellipse {:x -20 :y -10 :width 10 :height 10}]
                                [:ellipse {:x 20 :y -10 :width 10 :height 10}]]
                               [:fill {}
                                [:arc {:width 60 :height 60 :start 0 :stop 3.14}]]]]]
                            opts)
                          (play-cljs.core/draw-sketch! game renderer children opts)))]}
  (defonce smiley-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})
        var-obj focus]
    (doto smiley-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 150 y 150}} @*state]
                        (try
                          (render smiley-game [:smiley {:x 0 :y 0}])
                          (callback var-obj)
                          (catch js/Error e (callback e))))))))))

(s/def ::div (s/cat
               :name #{:div}
               :opts ::utils/basic-opts
               :children (s/* ::content)))

(defmethod draw-sketch! :div [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::div content)))
    (throw (js/Error. (s/explain-str ::div content))))
  (let [[_ opts & children] content
        opts (utils/update-opts opts parent-opts utils/basic-defaults)]
    (draw-sketch! game renderer children opts)))

(defexample :div
  {:doc "Acts as a generic container of options that it passes
down to its children. The `x` and `y` are special in this example,
serving as the pointer's position. Notice that the :rect is
hard-coded at (0,0) but the :div is passing its own position down."
   :with-card card
   :with-callback callback
   :with-focus [focus [:div {:x x :y y}
                       [:fill {:color "lightblue"}
                        [:rect {:x 0 :y 0 :width 100 :height 100}]]]]}
  (defonce div-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto div-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 50 y 50}} @*state]
                        (try
                          (let [content focus]
                            (render div-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::text (s/cat
                :name #{:text}
                :opts (s/merge
                        ::utils/basic-opts
                        ::utils/text-opts)
                :children (s/* ::content)))

(defmethod draw-sketch! :text [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::text content)))
    (throw (js/Error. (s/explain-str ::text content))))
  (let [[_ opts & children] content
        {:keys [value x y size font halign valign leading style] :as opts}
        (utils/update-opts opts parent-opts utils/text-defaults)]
    (doto renderer
      (.textSize size)
      (.textFont font)
      (.textAlign (utils/halign->constant renderer halign) (utils/valign->constant renderer valign))
      (.textLeading leading)
      (.textStyle (utils/style->constant renderer style))
      (.text value x y))
    (draw-sketch! game renderer children opts)))

(defexample :text
  {:doc "Draws text to the screen.
   
   :value  -  The text to display (string)
   :size   -  The font size (number)
   :font   -  The name of the font (string)
   :style  -  The font style (:normal, :italic, :bold)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:text {:value "Hello, world!"
                              :x 0 :y 50 :size 16
                              :font "Georgia" :style :italic}]]}
  (defonce text-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto text-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render text-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::arc (s/cat
               :name #{:arc}
               :opts ::utils/basic-opts
               :children (s/* ::content)))

(defmethod draw-sketch! :arc [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::arc content)))
    (throw (js/Error. (s/explain-str ::arc content))))
  (let [[_ opts & children] content
        {:keys [x y width height start stop] :as opts}
        (utils/update-opts opts parent-opts utils/basic-defaults)]
    (.arc renderer x y width height start stop)
    (draw-sketch! game renderer children opts)))

(defexample :arc
  {:doc "Draws an arc to the screen.
   
   :width  -  The width of the arc (number)
   :height -  The height of the arc (number)
   :start  -  Angle to start the arc, in radians (number)
   :stop   -  Angle to stop the arc, in radians (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:arc {:x 200 :y 0 :width 200 :height 200 :start 0 :stop 3.14}]]}
  (defonce arc-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto arc-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render arc-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::ellipse (s/cat
                   :name #{:ellipse}
                   :opts ::utils/basic-opts
                   :children (s/* ::content)))

(defmethod draw-sketch! :ellipse [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::ellipse content)))
    (throw (js/Error. (s/explain-str ::ellipse content))))
  (let [[_ opts & children] content
        {:keys [x y width height] :as opts}
        (utils/update-opts opts parent-opts utils/basic-defaults)]
    (.ellipse renderer x y width height)
    (draw-sketch! game renderer children opts)))

(defexample :ellipse
  {:doc "Draws an ellipse (oval) to the screen.
   
   :width  -  The width of the ellipse (number)
   :height -  The height of the ellipse (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:ellipse {:x 100 :y 100 :width 50 :height 70}]]}
  (defonce ellipse-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto ellipse-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render ellipse-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::line (s/cat
                :name #{:line}
                :opts ::utils/basic-opts
                :children (s/* ::content)))

(defmethod draw-sketch! :line [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::line content)))
    (throw (js/Error. (s/explain-str ::line content))))
  (let [[_ opts & children] content
        opts (utils/update-opts opts parent-opts utils/basic-defaults)
        {:keys [x1 y1 x2 y2] :as opts}
        (-> opts
            (update :x1 + (:x opts))
            (update :y1 + (:y opts))
            (update :x2 + (:x opts))
            (update :y2 + (:y opts)))]
    (.line renderer x1 y1 x2 y2)
    (draw-sketch! game renderer children opts)))

(defexample :line
  {:doc "Draws a line (a direct path between two points) to the screen.
   
   :x1  -  The x-coordinate of the first point (number)
   :y1  -  The y-coordinate of the first point (number)
   :x2  -  The x-coordinate of the second point (number)
   :y2  -  The y-coordinate of the second point (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:line {:x1 0 :y1 0 :x2 50 :y2 50}]]}
  (defonce line-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto line-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render line-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::point (s/cat
                 :name #{:point}
                 :opts ::utils/basic-opts
                 :children (s/* ::content)))

(defmethod draw-sketch! :point [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::point content)))
    (throw (js/Error. (s/explain-str ::point content))))
  (let [[_ opts & children] content
        {:keys [x y] :as opts}
        (utils/update-opts opts parent-opts utils/basic-defaults)]
    (.point renderer x y)
    (draw-sketch! game renderer children opts)))

(defexample :point
  {:doc "Draws a point, a coordinate in space at the dimension of one pixel."
   :with-card card
   :with-callback callback
   :with-focus [focus [[:point {:x 5 :y 5}]
                       [:point {:x 10 :y 5}]
                       [:point {:x 15 :y 5}]
                       [:point {:x 20 :y 5}]
                       [:point {:x 25 :y 5}]
                       [:point {:x 30 :y 5}]
                       [:point {:x 35 :y 5}]]]}
  (defonce point-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto point-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render point-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::quad (s/cat
                :name #{:quad}
                :opts ::utils/basic-opts
                :children (s/* ::content)))

(defmethod draw-sketch! :quad [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::quad content)))
    (throw (js/Error. (s/explain-str ::quad content))))
  (let [[_ opts & children] content
        opts (utils/update-opts opts parent-opts utils/basic-defaults)
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

(defexample :quad
  {:doc "Draw a quad. A quad is a quadrilateral, a four sided polygon.
   
   :x1  -  The x-coordinate of the first point (number)
   :y1  -  The y-coordinate of the first point (number)
   :x2  -  The x-coordinate of the second point (number)
   :y2  -  The y-coordinate of the second point (number)
   :x3  -  The x-coordinate of the third point (number)
   :y3  -  The y-coordinate of the third point (number)
   :x4  -  The x-coordinate of the fourth point (number)
   :y4  -  The y-coordinate of the fourth point (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:quad {:x1 50 :y1 55 :x2 70 :y2 15 :x3 10 :y3 15 :x4 20 :y4 55}]]}
  (defonce quad-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto quad-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render quad-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::rect (s/cat
                :name #{:rect}
                :opts ::utils/basic-opts
                :children (s/* ::content)))

(defmethod draw-sketch! :rect [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::rect content)))
    (throw (js/Error. (s/explain-str ::rect content))))
  (let [[_ opts & children] content
        {:keys [x y width height] :as opts}
        (utils/update-opts opts parent-opts utils/basic-defaults)]
    (.rect renderer x y width height)
    (draw-sketch! game renderer children opts)))

(defexample :rect
  {:doc "Draws a rectangle to the screen.
   A rectangle is a four-sided shape with every angle at ninety degrees.
   
   :width  -  The width of the rectangle (number)
   :height -  The height of the rectangle (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:rect {:x 10 :y 15 :width 20 :height 30}]]}
  (defonce rect-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto rect-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render rect-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::triangle (s/cat
                    :name #{:triangle}
                    :opts ::utils/basic-opts
                    :children (s/* ::content)))

(defmethod draw-sketch! :triangle [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::triangle content)))
    (throw (js/Error. (s/explain-str ::triangle content))))
  (let [[_ opts & children] content
        opts (utils/update-opts opts parent-opts utils/basic-defaults)
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

(defexample :triangle
  {:doc "A triangle is a plane created by connecting three points.
   
   :x1  -  The x-coordinate of the first point (number)
   :y1  -  The y-coordinate of the first point (number)
   :x2  -  The x-coordinate of the second point (number)
   :y2  -  The y-coordinate of the second point (number)
   :x3  -  The x-coordinate of the third point (number)
   :y3  -  The y-coordinate of the third point (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:triangle {:x1 10, :y1 10, :x2 50, :y2 25, :x3 10, :y3 35}]]}
  (defonce triangle-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto triangle-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render triangle-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::image (s/cat
                 :name #{:image}
                 :opts (s/merge
                         ::utils/basic-opts
                         ::utils/image-opts)
                 :children (s/* ::content)))

(defmethod draw-sketch! :image [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::image content)))
    (throw (js/Error. (s/explain-str ::image content))))
  (let [[_ opts & children] content
        {:keys [value name x y width height sx sy swidth sheight scale-x scale-y flip-x flip-y]
         :as opts} (utils/update-opts opts parent-opts utils/img-defaults)
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

(defexample :image
  {:doc "Displays an image.
   
   :name    -  The file name of the image (string)
   :width   -  The width of the image (number)
   :height  -  The height of the image (number)
   :sx      -  The x-coordinate of the subsection of the source image to draw into the destination rectangle (number)
   :sy      -  The y-coordinate of the subsection of the source image to draw into the destination rectangle (number)
   :swidth  -  The width of the subsection of the source image to draw into the destination rectangle (number)
   :sheight -  The height of the subsection of the source image to draw into the destination rectangle (number)
   :scale-x -  Percent to scale the image in the x-axis (number)
   :scale-y -  Percent to scale the image in the y-axis (number)
   :flip-x  -  Whether to flip the image on its x-axis (boolean)
   :flip-y  -  Whether to flip the image on its y-axis (boolean)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:image {:name "player_stand.png" :x 0 :y 0 :width 80 :height 80}]]}
  (defonce image-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto image-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render image-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::animation (s/cat
                     :name #{:animation}
                     :opts ::utils/basic-opts
                     :children (s/* ::content)))

(defmethod draw-sketch! :animation [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::animation content)))
    (throw (js/Error. (s/explain-str ::animation content))))
  (let [[_ opts & children] content
        {:keys [duration] :as opts} (utils/update-opts opts parent-opts utils/basic-defaults)
        images (vec children)
        cycle-time (mod (get-total-time game) (* duration (count images)))
        index (int (/ cycle-time duration))
        image (get images index)]
    (draw-sketch! game renderer image opts)))

(defexample :animation
  {:doc "Draws its children in a continuous loop.
   
   :duration  -  The number of milliseconds each child should be displayed (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:animation {:x 10 :y 10 :duration 200}
                       [:image {:name "player_walk1.png" :width 80 :height 80}]
                       [:image {:name "player_walk2.png" :width 80 :height 80}]
                       [:image {:name "player_walk3.png" :width 80 :height 80}]]]}
  (defonce animation-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto animation-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render animation-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::fill (s/cat
                :name #{:fill}
                :opts ::utils/basic-opts
                :children (s/* ::content)))

(defmethod draw-sketch! :fill [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::fill content)))
    (throw (js/Error. (s/explain-str ::fill content))))
  (let [[_ opts & children] content
        {:keys [grayscale color colors] :as opts}
        (utils/update-opts opts parent-opts utils/basic-defaults)]
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

(defexample :fill
  {:doc "Sets the color of the children.
   
   :color  -  The name of the color (string)
   :colors -  The RGB or HSB color values (vector of numbers)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:fill {:color "purple"}
                       [:rect {:x 40 :y 40 :width 150 :height 150}]]]}
  (defonce fill-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto fill-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render fill-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::stroke (s/cat
                  :name #{:stroke}
                  :opts ::utils/basic-opts
                  :children (s/* ::content)))

(defmethod draw-sketch! :stroke [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::stroke content)))
    (throw (js/Error. (s/explain-str ::stroke content))))
  (let [[_ opts & children] content
        {:keys [grayscale color colors] :as opts}
        (utils/update-opts opts parent-opts utils/basic-defaults)]
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

(defexample :stroke
  {:doc "Sets the color used to draw lines and borders around the children.
   
   :color  -  The name of the color (string)
   :colors -  The RGB or HSB color values (vector of numbers)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:stroke {:color "green"}
                       [:rect {:x 50 :y 50 :width 70 :height 70}]]]}
  (defonce stroke-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto stroke-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render stroke-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::bezier (s/cat
                  :name #{:bezier}
                  :opts ::utils/basic-opts
                  :children (s/* ::content)))

(defmethod draw-sketch! :bezier [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::bezier content)))
    (throw (js/Error. (s/explain-str ::bezier content))))
  (let [[_ opts & children] content
        opts (utils/update-opts opts parent-opts utils/basic-defaults)
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

(defexample :bezier
  {:doc "Draws a cubic Bezier curve on the screen.
   
   :x1  -  The x-coordinate of the first anchor point (number)
   :y1  -  The y-coordinate of the first anchor point (number)
   :x2  -  The x-coordinate of the first control point (number)
   :y2  -  The y-coordinate of the first control point (number)
   :x3  -  The x-coordinate of the second control point (number)
   :y3  -  The y-coordinate of the second control point (number)
   :x4  -  The x-coordinate of the second anchor point (number)
   :y4  -  The y-coordinate of the second anchor point (number)
   
   :z1  -  The z-coordinate of the first anchor point (number)
   :z2  -  The z-coordinate of the first control point (number)
   :z3  -  The z-coordinate of the second anchor point (number)
   :z4  -  The z-coordinate of the second control point (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:stroke {:colors [0 0 0]}
                       [:bezier {:x1 85 :y1 20 :x2 10 :y2 10 :x3 90 :y3 90 :x4 15 :y4 80}]]]}
  (defonce bezier-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto bezier-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render bezier-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::curve (s/cat
                 :name #{:curve}
                 :opts ::utils/basic-opts
                 :children (s/* ::content)))

(defmethod draw-sketch! :curve [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::curve content)))
    (throw (js/Error. (s/explain-str ::curve content))))
  (let [[_ opts & children] content
        opts (utils/update-opts opts parent-opts utils/basic-defaults)
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

(defexample :curve
  {:doc "Draws a curved line on the screen between two points,
   given as the middle four parameters.
   
   :x1  -  The x-coordinate of the beginning control point (number)
   :y1  -  The y-coordinate of the beginning control point (number)
   :x2  -  The x-coordinate of the first point (number)
   :y2  -  The y-coordinate of the first point (number)
   :x3  -  The x-coordinate of the second point (number)
   :y3  -  The y-coordinate of the second point (number)
   :x4  -  The x-coordinate of the ending control point (number)
   :y4  -  The y-coordinate of the ending control point (number)
   
   :z1  -  The z-coordinate of the beginning control point (number)
   :z2  -  The z-coordinate of the first point (number)
   :z3  -  The z-coordinate of the second point (number)
   :z4  -  The z-coordinate of the ending control point (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:stroke {:colors [255 102 0]}
                       [:curve {:x1 5 :y1 26 :x2 5 :y2 26 :x3 73 :y3 24 :x4 73 :y4 180}]]]}
  (defonce curve-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto curve-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render curve-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::rgb (s/cat
               :name #{:rgb}
               :opts (s/merge
                       ::utils/basic-opts
                       ::utils/rgb-opts)
               :children (s/* ::content)))

(defmethod draw-sketch! :rgb [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::rgb content)))
    (throw (js/Error. (s/explain-str ::rgb content))))
  (let [[_ opts & children] content
        {:keys [max-r max-g max-b max-a]} opts
        {:keys [max-red max-green max-blue max-alpha] :as opts}
        (utils/update-opts opts parent-opts utils/rgb-defaults)]
    (.push renderer)
    (.colorMode renderer
      (.-RGB renderer)
      (or max-r max-red)
      (or max-g max-green)
      (or max-b max-blue)
      (or max-a max-alpha))
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defexample :rgb
  {:doc "Causes the color values in all children to be interpreted as RGB colors.
   
   :max-red    -  Range for red (number between 0 and 255)
   :max-green  -  Range for green (number between 0 and 255)
   :max-blue   -  Range for blue (number between 0 and 255)
   :max-alpha  -  Range for alpha (number between 0 and 255)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:rgb {:max-red 100 :max-green 100 :max-blue 100}
                       [:fill {:colors [20 50 70]}
                        [:rect {:x 10 :y 10 :width 70 :height 70}]]]]}
  (defonce rgb-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto rgb-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render rgb-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::hsb (s/cat
               :name #{:hsb}
               :opts (s/merge
                       ::utils/basic-opts
                       ::utils/hsb-opts)
               :children (s/* ::content)))

(defmethod draw-sketch! :hsb [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::hsb content)))
    (throw (js/Error. (s/explain-str ::hsb content))))
  (let [[_ opts & children] content
        {:keys [max-h max-s max-b max-a]} opts
        {:keys [max-hue max-saturation max-brightness max-alpha] :as opts}
        (utils/update-opts opts parent-opts utils/hsb-defaults)]
    (.push renderer)
    (.colorMode renderer
      (.-HSB renderer)
      (or max-h max-hue)
      (or max-s max-saturation)
      (or max-b max-brightness)
      (or max-a max-alpha))
    (draw-sketch! game renderer children opts)
    (.pop renderer)))

(defexample :hsb
  {:doc "Causes the color values in all children to be interpreted as HSB colors.
   
   :max-hue         -  Range for hue (number between 0 and 360)
   :max-saturation  -  Range for saturation (number between 0 and 100)
   :max-brightness  -  Range for brightness (number between 0 and 100)
   :max-alpha       -  Range for alpha (number between 0 and 255)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:hsb {:max-hue 100 :max-saturation 100 :max-brightness 100}
                       [:fill {:colors [20 50 70]}
                        [:rect {:x 10 :y 10 :width 70 :height 70}]]]]}
  (defonce hsb-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto hsb-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render hsb-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::tiled-map (s/cat
                     :name #{:tiled-map}
                     :opts ::utils/basic-opts
                     :children (s/* ::content)))

(defmethod draw-sketch! :tiled-map [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::tiled-map content)))
    (throw (js/Error. (s/explain-str ::tiled-map content))))
  (let [[_ opts & children] content
        {:keys [value name x y] :as opts}
        (utils/update-opts opts parent-opts utils/basic-defaults)
        ^js/p5.TiledMap value (or value
                                  (get-asset game name)
                                  (load-tiled-map game name))]
    (.draw value x y)
    (draw-sketch! game renderer children opts)))

; TODO: tiled-map examaple

(s/def ::shape (s/cat
                 :name #{:shape}
                 :opts ::utils/basic-opts
                 :children (s/* ::content)))

(defmethod draw-sketch! :shape [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::shape content)))
    (throw (js/Error. (s/explain-str ::shape content))))
  (let [[_ opts & children] content
        opts (utils/update-opts opts parent-opts utils/basic-defaults)
        points (:points opts)]
    (cond
      (odd? (count points))
      (throw ":shape requires :points to contain a seq'able with an even number of values (x and y pairs)")
      :else
      (do (.beginShape renderer)
          (loop [[x y & rest] points]
            (.vertex renderer x y)
            (when rest
              (recur rest)))
          (draw-sketch! game renderer children opts)
          (.endShape renderer (.-CLOSE renderer))))))

(defexample :shape
  {:doc "Draws a complex shape.
   
   :points  -  The x and y vertexes to draw (vector of numbers)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:shape {:points [30 20 85 20 85 75 30 75]}]]}
  (defonce shape-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto shape-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render shape-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(s/def ::contour (s/cat
                   :name #{:contour}
                   :opts ::utils/basic-opts
                   :children (s/* ::content)))

(defmethod draw-sketch! :contour [game ^js/p5 renderer content parent-opts]
  (when (and (get-option game :debug?)
             (= :cljs.spec.alpha/invalid (s/conform ::contour content)))
    (throw (js/Error. (s/explain-str ::contour content))))
  (let [[_ opts & children] content
        opts (utils/update-opts opts parent-opts utils/basic-defaults)
        points (:points opts)]
    (cond
      (odd? (count points))
      (throw ":contour requires :points to contain a seq'able with an even number of values (x and y pairs)")
      :else
      (do (.beginContour renderer)
        (loop [[x y & rest] points]
          (.vertex renderer x y)
          (when rest
            (recur rest)))
        (draw-sketch! game renderer children opts)
        (.endContour renderer (.-CLOSE renderer))))))

(defexample :contour
  {:doc "Draws a negative shape.
   
   :points  -  The x and y vertexes to draw (vector of numbers)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:shape {:points [40 40 80 40 80 80 40 80]}
                       [:contour {:points [20 20 20 40 40 40 40 20]}]]]}
  (defonce contour-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto contour-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render contour-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(defmethod draw-sketch! :default [game ^js/p5 renderer content parent-opts]
  (when-let [name (first content)]
    (throw (js/Error. (str "Command not found: " name)))))

(s/def ::command (s/cat
                   :name keyword?
                   :opts ::utils/basic-opts
                   :children (s/* ::content)))

(s/def ::content (s/or
                   :single (s/nilable ::command)
                   :multiple (s/* ::content)))

(defn create-game
  "Returns a game object."
  ([width height]
   (create-game width height {}))
  ([width height {:keys [parent debug?]
                  :or {debug? (not js/COMPILED)}}]
   (if debug?
     (js/console.log
       (str "Debugging is enabled. If things are slow, try passing "
         "`:debug? false` to the options map of `create-game`."))
     (set! (.-disableFriendlyErrors ^js/p5 js/p5) true))
   (let [*hidden-state (atom {:screen nil
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
         (run! events/unlistenByKey (:listeners @*hidden-state))
         (swap! *hidden-state assoc :listeners [])
         (js/p5.
           (fn [^js/p5 renderer]
             (set! (.-setup renderer)
               (fn []
                 ;; create the canvas
                 (let [^js/p5 canvas-wrapper (cond-> (.createCanvas renderer width height)
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
           (draw-sketch! this renderer content {})))
       (pre-render [this image-name width height content]
         (when-let [^js/p5 renderer (get-renderer this)]
           (let [object (.createGraphics renderer width height)]
             (draw-sketch! this object content {})
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
         (get-in @*hidden-state [:assets name]))
       (get-option [game k]
         (get {:parent parent
               :debug? debug?}
           k))))))
