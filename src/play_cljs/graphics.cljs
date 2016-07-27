(ns play-cljs.graphics
  (:require [cljsjs.pixi]
            [play-cljs.core :as c]))

(defmulti draw-graphics! (fn [command _ _ _] (first command)))

(defmethod draw-graphics! :fill [command origin-x origin-y graphics]
  (let [[_ opts & children] command
        {:keys [color alpha]} opts]
    (.beginFill graphics color alpha)
    (draw-graphics! children origin-x origin-y graphics)
    (.endFill graphics)))

(defmethod draw-graphics! :circle [command origin-x origin-y graphics]
  (let [[_ opts & children] command
        {:keys [x y radius]} opts]
    (.drawCircle graphics (+ origin-x x) (+ origin-y y) radius)))

(defmethod draw-graphics! :ellipse [command origin-x origin-y graphics]
  (let [[_ opts & children] command
        {:keys [x y width height]} opts]
    (.drawEllipse graphics (+ origin-x x) (+ origin-y y) width height)))

(defmethod draw-graphics! :polygon [command origin-x origin-y graphics]
  (let [[_ opts & children] command
        {:keys [path]} opts]
    (let [path (->> path
                    (partition 2)
                    (map (fn [[x y]] [(+ origin-x x) (+ origin-y y)]))
                    flatten)]
      (.drawPolygon graphics (into-array path)))))

(defmethod draw-graphics! :rect [command origin-x origin-y graphics]
  (let [[_ opts & children] command
        {:keys [x y width height radius]} opts]
    (.drawRect graphics (+ origin-x x) (+ origin-y y) width height)))

(defmethod draw-graphics! :rounded-rect [command origin-x origin-y graphics]
  (let [[_ opts & children] command
        {:keys [x y width height radius]} opts]
    (.drawRoundedRect graphics (+ origin-x x) (+ origin-y y) width height radius)))

(defmethod draw-graphics! :default [command origin-x origin-y graphics]
  (if (sequential? (first command))
    (run! #(draw-graphics! % origin-x origin-y graphics) command)
    (throw (js/Error. (str "Invalid graphics command: " (pr-str command))))))

(defrecord Graphics [command x y] c/Command
  (run [this game]
    (let [renderer (c/get-renderer game)
          graphics (js/PIXI.Graphics.)]
      (draw-graphics! command x y graphics)
      (.render renderer graphics))))

(defn graphics
  ([command]
   (graphics command 0 0))
  ([command x y]
   (Graphics. command x y)))

