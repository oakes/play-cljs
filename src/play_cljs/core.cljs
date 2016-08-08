(ns play-cljs.core
  (:require [goog.events :as events]
            [p5.core]
            [play-cljs.sketch :as s]))

(defprotocol Screen
  (on-show [this])
  (on-hide [this])
  (on-render [this])
  (on-event [this event]))

(defprotocol Game
  (start [this events])
  (stop [this])
  (render [this content])
  (load-image [this path])
  (get-screens [this])
  (set-screens [this screens])
  (get-screen [this])
  (set-screen [this screen])
  (get-renderer [this])
  (set-renderer [this renderer])
  (get-canvas [this])
  (get-total-time [this])
  (get-delta-time [this])
  (get-pressed-keys [this])
  (get-width [this])
  (get-height [this])
  (set-size [this width height]))

(defn create-game [width height]
  (let [renderer (js/p5. (fn [renderer]))
        hidden-state-atom (atom {:screens []
                                 :renderer renderer
                                 :canvas nil
                                 :total-time 0
                                 :delta-time 0
                                 :pressed-keys #{}})]
    (reify Game
      (start [this events]
        (set! (.-setup renderer)
          (fn []
            (let [canvas (.-canvas (.createCanvas renderer width height))]
              (.removeAttribute canvas "style")
              (swap! hidden-state-atom assoc :canvas canvas))))
        (set! (.-draw renderer)
          (fn []
            (swap! hidden-state-atom
              (fn [hidden-state]
                (let [time (.millis renderer)]
                  (assoc hidden-state
                    :total-time time
                    :delta-time (- time (:total-time hidden-state))))))
            (.clear renderer)
            (run! on-render (get-screens this))))
        (doto js/window
          (events/listen "keydown" #(swap! hidden-state-atom update :pressed-keys conj (.-keyCode %)))
          (events/listen "keyup" #(if (contains? #{91 93} (.-keyCode %))
                                    (swap! hidden-state-atom assoc :pressed-keys #{})
                                    (swap! hidden-state-atom update :pressed-keys disj (.-keyCode %))))
          (events/listen "blur" #(swap! hidden-state-atom assoc :pressed-keys #{})))
        (doseq [event events]
          (events/listen js/window event (fn [e]
                                           (run! #(on-event % e) (get-screens this))))))
      (stop [this]
        (events/removeAll js/window))
      (render [this content]
        (s/draw-sketch! (get-renderer this) content {}))
      (load-image [this path]
        (.loadImage (get-renderer this) path))
      (get-screens [this]
        (:screens @hidden-state-atom))
      (set-screens [this screens]
        (run! on-hide (get-screens this))
        (swap! hidden-state-atom assoc :screens screens)
        (run! on-show (get-screens this)))
      (get-screen [this]
        (first (get-screens this)))
      (set-screen [this screen]
        (set-screens this [screen]))
      (get-renderer [this]
        (:renderer @hidden-state-atom))
      (set-renderer [this renderer]
        (swap! hidden-state-atom assoc :renderer renderer)
        renderer)
      (get-canvas [this]
        (:canvas @hidden-state-atom))
      (get-total-time [this]
        (:total-time @hidden-state-atom))
      (get-delta-time [this]
        (:delta-time @hidden-state-atom))
      (get-pressed-keys [this]
        (:pressed-keys @hidden-state-atom))
      (get-width [this]
        (.-width (get-renderer this)))
      (get-height [this]
        (.-height (get-renderer this)))
      (set-size [this width height]
        (.resizeCanvas (get-renderer this) width height)))))

