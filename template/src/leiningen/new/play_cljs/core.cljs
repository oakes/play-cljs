(ns {{name}}.{{core-name}}
  (:require [play-cljs.core :as p]
            [goog.events :as events])
  (:require-macros [{{name}}.music :refer [build-for-cljs]]))

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

(events/listen js/window "mousemove"
  (fn [event]
    (swap! state assoc :text-x (.-clientX event) :text-y (.-clientY event))))

(events/listen js/window "resize"
  (fn [event]
    (p/set-size game js/window.innerWidth js/window.innerHeight)))

;; start the game

(doto game
  (p/start)
  (p/set-screen main-screen))

;; build music, put it in the audio tag, and make the button toggle it on and off

(defonce play-music? (atom false))

(defonce audio (js/document.querySelector "#audio"))
(set! (.-src audio) (build-for-cljs))

(defonce button (js/document.querySelector "#audio-button"))
(set! (.-onclick button)
      (fn [e]
        (if (swap! play-music? not)
          (.play audio)
          (.pause audio))))
