(ns play-cljs.graphics)

(defmulti draw-graphics! (fn [_ content _ _] (first content)))

(defmethod draw-graphics! :fill [object content origin-x origin-y]
  (let [[_ opts & children] content
        {:keys [color alpha]} opts]
    (.beginFill object color alpha)
    (draw-graphics! object children origin-x origin-y)
    (.endFill object)))

(defmethod draw-graphics! :circle [object content origin-x origin-y]
  (let [[_ opts & children] content
        {:keys [x y radius]} opts
        x (+ origin-x x)
        y (+ origin-y y)]
    (.drawCircle object x y radius)
    (draw-graphics! object children x y)))

(defmethod draw-graphics! :ellipse [object content origin-x origin-y]
  (let [[_ opts & children] content
        {:keys [x y width height]} opts
        x (+ origin-x x)
        y (+ origin-y y)]
    (.drawEllipse object x y width height)
    (draw-graphics! object children x y)))

(defmethod draw-graphics! :polygon [object content origin-x origin-y]
  (let [[_ opts & children] content
        {:keys [path]} opts
        path (->> path
                  (partition 2)
                  (map (fn [[x y]] [(+ origin-x x) (+ origin-y y)]))
                  flatten)]
    (.drawPolygon object (into-array path))
    (draw-graphics! object children origin-x origin-y)))

(defmethod draw-graphics! :rect [object content origin-x origin-y]
  (let [[_ opts & children] content
        {:keys [x y width height radius]} opts
        x (+ origin-x x)
        y (+ origin-y y)]
    (.drawRect object x y width height)
    (draw-graphics! object children x y)))

(defmethod draw-graphics! :rounded-rect [object content origin-x origin-y]
  (let [[_ opts & children] content
        {:keys [x y width height radius]} opts
        x (+ origin-x x)
        y (+ origin-y y)]
    (.drawRoundedRect object x y width height radius)
    (draw-graphics! object children x y)))

(defmethod draw-graphics! :default [object content origin-x origin-y]
  (cond
    (sequential? (first content))
    (run! #(draw-graphics! object % origin-x origin-y) content)
    (nil? (first content))
    nil
    :else
    (throw (js/Error. (str "Invalid graphics command: " (pr-str content))))))

