(ns {{namespace}}
  (:require [play-cljs.core :as p]))

(defonce game (p/create-game 500 500))
(defonce state (atom {}))

(def main-screen
  (reify p/Screen
    (on-show [this]
      (reset! state {:text-x 20 :text-y 30}))
    (on-hide [this])
    (on-render [this]
      (p/render game
        [[:fill {:color "lightblue"}
          [:rect {:x 0 :y 0 :width 500 :height 500}]]
         [:fill {:color "black"}
          ["Hello, world!" {:x (:text-x @state) :y (:text-y @state) :size 16 :font "Georgia" :style :italic}]]])
      (swap! state update :text-x inc))
    (on-event [this event])))

(doto game
  (p/stop)
  (p/start ["keydown"])
  (p/set-screen main-screen))

