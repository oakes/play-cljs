(ns {{namespace}}
  (:require [play-cljs.core :as p]))

(declare game)

(def main-screen
  (reify p/Screen
    (on-show [this state]
      (p/set-state game
        (assoc state :text-x 20 :text-y 30)))
    (on-hide [this state])
    (on-render [this state]
      (p/render game
        [[:fill {:color "lightblue"}
          [:rect {:x 0 :y 0 :width 500 :height 500}]]
         [:fill {:color "black"}
          ["Hello, world!" {:x (:text-x state) :y (:text-y state) :size 16 :font "Georgia" :style :italic}]]])
      (p/set-state game
        (update state :text-x inc)))
    (on-event [this state event])))

(defonce game (p/create-game 500 500))

(doto game
  (p/stop)
  (p/start ["keydown"])
  (p/set-screen main-screen))

