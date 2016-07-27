(ns play-cljs.graphics
  (:require [cljsjs.pixi]
            [play-cljs.core :as c]))

(defmulti draw-graphics! (fn [command graphics] (first command)))

(defmethod draw-graphics! :fill [command graphics]
  (let [[_ opts & children] command
        {:keys [color alpha]} opts]
    (.beginFill graphics color alpha)
    (draw-graphics! children graphics)
    (.endFill graphics)))

(defmethod draw-graphics! :circle [command graphics]
  (let [[_ opts & children] command
        {:keys [x y radius]} opts]
    (.drawCircle graphics x y radius)))

(defmethod draw-graphics! :ellipse [command graphics]
  (let [[_ opts & children] command
        {:keys [x y width height]} opts]
    (.drawEllipse graphics x y width height)))

(defmethod draw-graphics! :polygon [command graphics]
  (let [[_ opts & children] command
        {:keys [path]} opts]
    (.drawPolygon graphics (into-array path))))

(defmethod draw-graphics! :rect [command graphics]
  (let [[_ opts & children] command
        {:keys [x y width height radius]} opts]
    (.drawRect graphics x y width height)))

(defmethod draw-graphics! :rounded-rect [command graphics]
  (let [[_ opts & children] command
        {:keys [x y width height radius]} opts]
    (.drawRoundedRect graphics x y width height radius)))

(defmethod draw-graphics! :default [command graphics]
  (if (sequential? (first command))
    (run! #(draw-graphics! % graphics) command)
    (throw (js/Error. (str "Invalid graphics command: " (pr-str command))))))

(defrecord Graphics [command] c/Command
  (run [this game]
    (let [renderer (c/get-renderer game)
          graphics (js/PIXI.Graphics.)]
      (draw-graphics! command graphics)
      (.render renderer graphics))))

(defn graphics [command]
  (Graphics. command))

