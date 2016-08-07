(ns play-cljs.sketch)

(defmulti draw-sketch! (fn [renderer content parent-opts]
                         (let [command (first content)]
                           (if (string? command)
                             :text
                             command))))

(defmethod draw-sketch! :text [renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [x y size]
         :or {x 0 y 0 size 32}} opts
        x (+ x (or (:x parent-opts) 0))
        y (+ y (or (:y parent-opts) 0))
        opts (assoc opts :x x :y y)]
    (.textSize renderer size)
    (.text renderer command x y)
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :default [renderer content parent-opts]
  (cond
    (sequential? (first content))
    (run! #(draw-sketch! renderer % parent-opts) content)
    (nil? (first content))
    nil
    :else
    (throw (js/Error. (str "Invalid sketch command: " (pr-str content))))))

