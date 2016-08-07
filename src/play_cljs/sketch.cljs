(ns play-cljs.sketch)

(defmulti draw-sketch! (fn [renderer content parent-opts]
                         (let [command (first content)]
                           (if (string? command)
                             :text
                             command))))

(def ^:const text-defaults {:x 0 :y 0 :size 32 :font "Helvetica"})

(defmethod draw-sketch! :text [renderer content parent-opts]
  (let [[command opts & children] content
        parent-opts (merge text-defaults parent-opts)
        opts (-> (merge text-defaults (select-keys parent-opts [:size :font]) opts)
                 (update :x + (:x parent-opts))
                 (update :y + (:y parent-opts)))
        {:keys [x y size font]} opts]
    (.textSize renderer size)
    (.textFont renderer font)
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

