(ns play-cljs.examples
  (:require-macros [dynadoc.example :refer [defexample]]))

(defexample play-cljs.core/get-renderer
  {:doc "After retrieving the p5 object, we can call any built-in p5 functions on it."
   :with-card card
   :with-callback callback
   :with-focus [focus (let [p5 (get-renderer game)]
                        (.directionalLight p5 (.color p5 250 250 250) (.createVector p5 100 0 0)))]}
  (defonce game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true :mode :webgl}))
  (let [*state (atom {})]
    (doto game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 150 y 150}} @*state]
                        (try
                          (callback focus)
                          (render game [:sphere {:radius 50}])
                          (catch js/Error e (callback e))))))))))

(defexample play-cljs.core/draw-sketch!
  {:doc "Extending this multimethod allows you to create new entity types.
   In this example, we create a new entity type called :smiley that draws a smiley face.
   After defining the method, it can be rendered like this: [:smiley {:x 0 :y 0}]"
   :with-card card
   :with-callback callback
   :with-focus [focus (defmethod play-cljs.core/draw-sketch! :smiley [game ^js/p5 renderer content parent-opts]
                        (let [[_ opts & children] content
                              opts (play-cljs.options/update-opts opts parent-opts play-cljs.options/basic-defaults)]
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

(defexample :play-cljs.core/div
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

(defexample :play-cljs.core/rotate
  {:doc "Rotates a shape the amount specified by the angle parameter.
   
   :angle  -  The angle of rotation, in radians (number)
   :axis   -  The axis to rotate on (:x, :y, or :z) (:webgl mode only)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:rotate {:x 100 :y 100 :angle (/ (js/window.performance.now) 1000)}
                       [:rect {:x 0 :y 0 :width 50 :height 50}]]]}
  (defonce rotate-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto rotate-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render rotate-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

;; 2d

(defexample :play-cljs.core/text
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

(defexample :play-cljs.core/arc
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

(defexample :play-cljs.core/ellipse
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

(defexample :play-cljs.core/line
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

(defexample :play-cljs.core/point
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

(defexample :play-cljs.core/quad
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

(defexample :play-cljs.core/rect
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

(defexample :play-cljs.core/triangle
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

(defexample :play-cljs.core/image
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
   :flip-x? -  Whether to flip the image on its x-axis (boolean)
   :flip-y? -  Whether to flip the image on its y-axis (boolean)"
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

(defexample :play-cljs.core/animation
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

(defexample :play-cljs.core/fill
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

(defexample :play-cljs.core/stroke
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

(defexample :play-cljs.core/bezier
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

(defexample :play-cljs.core/curve
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

(defexample :play-cljs.core/rgb
  {:doc "Causes the color values in all children to be interpreted as RGB colors.
   
   :max-r  -  Range for red (number between 0 and 255)
   :max-g  -  Range for green (number between 0 and 255)
   :max-b  -  Range for blue (number between 0 and 255)
   :max-a  -  Range for alpha (number between 0 and 255)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:rgb {:max-r 64 :max-g 64 :max-b 64}
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

(defexample :play-cljs.core/hsb
  {:doc "Causes the color values in all children to be interpreted as HSB colors.
   
   :max-h  -  Range for hue (number between 0 and 360)
   :max-s  -  Range for saturation (number between 0 and 100)
   :max-b  -  Range for brightness (number between 0 and 100)
   :max-a  -  Range for alpha (number between 0 and 255)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:hsb {:max-h 90 :max-s 50 :max-b 100}
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

(defexample :play-cljs.core/hsl
  {:doc "Causes the color values in all children to be interpreted as HSL colors.
   
   :max-h  -  Range for hue (number between 0 and 360)
   :max-s  -  Range for saturation (number between 0 and 100)
   :max-l  -  Range for lightness (number between 0 and 100)
   :max-a  -  Range for alpha (number between 0 and 255)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:hsl {:max-h 90 :max-s 50 :max-l 100}
                       [:fill {:colors [20 50 70]}
                        [:rect {:x 10 :y 10 :width 70 :height 70}]]]]}
  (defonce hsl-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true}))
  (let [*state (atom {})]
    (doto hsl-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render hsl-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(defexample :play-cljs.core/shape
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

(defexample :play-cljs.core/contour
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

;; 3d

(defexample :play-cljs.core/plane
  {:doc "[3D] Draws a plane.
   
   NOTE: You must pass {:mode :webgl} to the third argument of create-game.
   
   :width     -  The width of the plane (number)
   :height    -  The height of the plane (number)
   :detail-x  -  Triangle subdivisions in the x-dimension (number)
   :detail-y  -  Triangle subdivisions in the y-dimension (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :x}
                       [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :y}
                        [:plane {:width 50 :height 50}]]]]}
  (defonce plane-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true :mode :webgl}))
  (let [*state (atom {})]
    (doto plane-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render plane-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(defexample :play-cljs.core/box
  {:doc "[3D] Draws a box.
   
   NOTE: You must pass {:mode :webgl} to the third argument of create-game.
   
   :width     -  The width of the box (number)
   :height    -  The height of the box (number)
   :depth     -  The depth of the box (number)
   :detail-x  -  Triangle subdivisions in the x-dimension (number)
   :detail-y  -  Triangle subdivisions in the y-dimension (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :x}
                       [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :y}
                        [:box {:width 50 :height 50 :depth 50}]]]]}
  (defonce box-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true :mode :webgl}))
  (let [*state (atom {})]
    (doto box-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render box-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(defexample :play-cljs.core/sphere
  {:doc "[3D] Draws a sphere.
   
   NOTE: You must pass {:mode :webgl} to the third argument of create-game.
   
   :radius    -  The radius of the circle (number)
   :detail-x  -  Number of segments in the x-dimension (number)
   :detail-y  -  Number of segments in the y-dimension (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :x}
                       [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :y}
                        [:sphere {:radius 50}]]]]}
  (defonce sphere-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true :mode :webgl}))
  (let [*state (atom {})]
    (doto sphere-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render sphere-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(defexample :play-cljs.core/cylinder
  {:doc "[3D] Draws a cylinder.
   
   NOTE: You must pass {:mode :webgl} to the third argument of create-game.
   
   :radius       -  The radius of the cylinder (number)
   :height       -  The height of the cylinder (number)
   :detail-x     -  Number of segments in the x-dimension (number)
   :detail-y     -  Number of segments in the y-dimension (number)
   :bottom-cap?  -  Whether to draw the bottom of the cylinder (boolean)
   :top-cap?     -  Whether to draw the top of the cylinder (boolean)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :x}
                       [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :y}
                        [:cylinder {:radius 50 :height 100}]]]]}
  (defonce cylinder-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true :mode :webgl}))
  (let [*state (atom {})]
    (doto cylinder-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render cylinder-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(defexample :play-cljs.core/cone
  {:doc "[3D] Draws a cone.
   
   NOTE: You must pass {:mode :webgl} to the third argument of create-game.
   
   :radius    -  The radius of the cone (number)
   :height    -  The height of the cone (number)
   :detail-x  -  Number of segments in the x-dimension (number)
   :detail-y  -  Number of segments in the y-dimension (number)
   :cap?      -  Whether to draw the base of the cone (boolean)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :x}
                       [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :y}
                        [:cone {:radius 50 :height 100}]]]]}
  (defonce cone-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true :mode :webgl}))
  (let [*state (atom {})]
    (doto cone-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render cone-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(defexample :play-cljs.core/ellipsoid
  {:doc "[3D] Draws an ellipsoid.
   
   NOTE: You must pass {:mode :webgl} to the third argument of create-game.
   
   :radius    -  The radius of the ellipsoid (number)
   :height    -  The height of the ellipsoid (number)
   :detail-x  -  Number of segments in the x-dimension (number)
   :detail-y  -  Number of segments in the y-dimension (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :x}
                       [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :y}
                        [:ellipsoid {:radius-x 20 :radius-y 30 :radius-z 40}]]]]}
  (defonce ellipsoid-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true :mode :webgl}))
  (let [*state (atom {})]
    (doto ellipsoid-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render ellipsoid-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(defexample :play-cljs.core/torus
  {:doc "[3D] Draws a torus.
   
   NOTE: You must pass {:mode :webgl} to the third argument of create-game.
   
   :radius       -  The radius of the whole ring (number)
   :tube-radius  -  The radius of the tube (number)
   :detail-x     -  Number of segments in the x-dimension (number)
   :detail-y     -  Number of segments in the y-dimension (number)"
   :with-card card
   :with-callback callback
   :with-focus [focus [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :x}
                       [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :y}
                        [:torus {:radius 50 :tube-radius 15}]]]]}
  (defonce torus-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true :mode :webgl}))
  (let [*state (atom {})]
    (doto torus-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render torus-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))

(defexample :play-cljs.core/model
  {:doc "[3D] Draws a model.
   
   NOTE: You must pass {:mode :webgl} to the third argument of create-game.
   
   :name       -  The file name of the model (string)
   :scale-x    -  Percent to scale the model in the x-axis (number)
   :scale-y    -  Percent to scale the model in the y-axis (number)
   :scale-z    -  Percent to scale the model in the z-axis (number)
   :normalize? -  When first loaded, resize the model to a standardized scale (boolean)
                  Changing this option has no effect if the model has already loaded"
   :with-card card
   :with-callback callback
   :with-focus [focus [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :x}
                       [:rotate {:angle (/ (js/window.performance.now) 1000) :axis :y}
                        [:model {:name "chr_old.obj" :normalize? true}]]]]}
  (defonce model-game (create-game (.-clientWidth card) (.-clientHeight card) {:parent card :debug? true :mode :webgl}))
  (let [*state (atom {})]
    (doto model-game
      (start-example-game card *state)
      (set-screen (reify Screen
                    (on-show [this])
                    (on-hide [this])
                    (on-render [this]
                      (let [{:keys [x y] :or {x 0 y 0}} @*state]
                        (try
                          (let [content focus]
                            (render model-game content)
                            (callback content))
                          (catch js/Error e (callback e))))))))))


