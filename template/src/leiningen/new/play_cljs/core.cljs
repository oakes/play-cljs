(ns {{namespace}}
  (:require [play-cljs.core :as p]))

(def view-size 500)

(def main-screen
  (reify p/Screen
    (on-show [this state]
      (p/reset-state
        (assoc state
          :label (p/text "Hello, world!" {:x 0 :y 0 :fill 0xFFFFFF})
          :background (p/graphics
                        [:fill {:color 0x8080FF :alpha 1}
                         [:rect {:x 0 :y 0 :width view-size :height view-size}]]))))
    (on-hide [this state])
    (on-render [this state]
      [(:background state)
       (:label state)])
    (on-event [this state event])))

(def canvas (.querySelector js/document "#canvas"))

(defonce renderer
  (p/create-renderer view-size view-size {:view canvas}))

(defonce game (p/create-game renderer))

(doto game
  (p/stop)
  (p/start ["keydown"])
  (p/set-screen main-screen))

