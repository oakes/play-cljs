(ns {{namespace}}
  (:require [play-cljs.core :as p]))

(defonce game (p/create-game (.-innerWidth js/window) (.-innerHeight js/window)))
(defonce state (atom {}))

(def main-screen
  (reify p/Screen
    (on-show [this]
      (reset! state {:text-x 20 :text-y 30}))
    (on-hide [this])
    (on-render [this]
      (p/render game
        [[:fill {:color "lightblue"}
          [:rect {:x 0 :y 0 :width (.-innerWidth js/window) :height (.-innerHeight js/window)}]]
         [:fill {:color "black"}
          [:text {:value "Hello, world!" :x (:text-x @state) :y (:text-y @state) :size 16 :font "Georgia" :style :italic}]]]))))

(doto game
  (p/start)
  (p/listen "mousemove"
    (fn [event]
      (swap! state assoc :text-x (.-clientX event) :text-y (.-clientY event))))
  (p/listen "resize"
    (fn [event]
      (p/set-size game (.-innerWidth js/window) (.-innerHeight js/window))))
  (p/set-screen main-screen))

