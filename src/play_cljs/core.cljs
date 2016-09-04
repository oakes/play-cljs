(ns play-cljs.core
  (:require [goog.events :as events]
            [p5.core]
            [p5.tiled-map]
            [play-cljs.sketch :as s]
            [cljs.core.async :refer [promise-chan put! <!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defprotocol Screen
  (on-show [this])
  (on-hide [this])
  (on-render [this])
  (on-event [this event]))

(defprotocol Game
  (start [this events])
  (stop [this])
  (render [this content])
  (pre-render [this width height content])
  (load-image [this path])
  (load-tiled-map [this map-name])
  (get-screens [this])
  (set-screens [this screens])
  (get-screen [this])
  (set-screen [this screen])
  (get-renderer [this])
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
                                 :canvas nil
                                 :total-time 0
                                 :delta-time 0
                                 :pressed-keys #{}})
        setup-finished? (promise-chan)
        preloads (atom [])]
    (reify Game
      (start [this events]
        (set! (.-setup renderer)
          (fn []
            ; create the canvas
            (let [canvas (.-canvas (.createCanvas renderer width height))]
              (.removeAttribute canvas "style")
              (swap! hidden-state-atom assoc :canvas canvas))
            ; allow on-show to be run
            (put! setup-finished? true)))
        ; events
        (doto js/window
          (events/listen "keydown" #(swap! hidden-state-atom update :pressed-keys conj (.-keyCode %)))
          (events/listen "keyup" #(if (contains? #{91 93} (.-keyCode %))
                                    (swap! hidden-state-atom assoc :pressed-keys #{})
                                    (swap! hidden-state-atom update :pressed-keys disj (.-keyCode %))))
          (events/listen "blur" #(swap! hidden-state-atom assoc :pressed-keys #{})))
        (doseq [event events]
          (events/listen js/window event (fn [e]
                                           (when (get-canvas this) ; only run after on-show
                                             (run! #(on-event % e) (get-screens this)))))))
      (stop [this]
        (events/removeAll js/window))
      (render [this content]
        (s/draw-sketch! (get-renderer this) content {}))
      (pre-render [this width height content]
        (doto (.createGraphics renderer width height)
          (s/draw-sketch! content {})))
      (load-image [this path]
        (let [finished-loading? (promise-chan)]
          (swap! preloads conj finished-loading?)
          (.loadImage renderer path #(put! finished-loading? true))))
      (load-tiled-map [this map-name]
        (let [finished-loading? (promise-chan)]
          (swap! preloads conj finished-loading?)
          (.loadTiledMap renderer map-name #(put! finished-loading? true))))
      (get-screens [this]
        (:screens @hidden-state-atom))
      (set-screens [this screens]
        (go
          ; wait for the setup function to finish
          (<! setup-finished?)
          ; change the screens
          (run! on-hide (get-screens this))
          (swap! hidden-state-atom assoc :screens screens)
          (run! on-show (get-screens this))
          ; wait for any assets from on-show to finish loading
          (doseq [finished-loading? @preloads]
            (<! finished-loading?))
          (reset! preloads [])
          ; set the draw function
          (set! (.-draw renderer)
            (fn []
              (swap! hidden-state-atom
                (fn [hidden-state]
                  (let [time (.millis renderer)]
                    (assoc hidden-state
                      :total-time time
                      :delta-time (- time (:total-time hidden-state))))))
              (.clear renderer)
              (run! on-render (get-screens this))))))
      (get-screen [this]
        (first (get-screens this)))
      (set-screen [this screen]
        (set-screens this [screen]))
      (get-renderer [this]
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
        (.-width renderer))
      (get-height [this]
        (.-height renderer))
      (set-size [this width height]
        (.resizeCanvas renderer width height)))))

