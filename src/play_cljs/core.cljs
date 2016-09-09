(ns play-cljs.core
  (:require [goog.events :as events]
            [p5.core]
            [p5.tiled-map]
            [play-cljs.sketch :as s]
            [cljs.core.async :refer [promise-chan put! <!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defprotocol Screen
  "A screen object provides the basic lifecycle for a game.
Simple games may only need to have one screen. They are a useful way to
isolate different aspects of your game. For example, you could make one
screen display the title and menu, and another screen contain the game
itself.

You can also display multiple screens at once. For example, the main
view of your game could be in one screen, and a second screen could
contain things you want to overlay on top of it, like your health
bar or a dialog.

You can create a screen by using `reify` like this:

```
(def main-screen
  (reify p/Screen
    (on-show [this])
    (on-hide [this])
    (on-render [this])
    (on-event [this event])))
```"
  (on-show [this]
    "Runs once, when the screen first appears.")
  (on-hide [this]
    "Runs once, when the screen is no longer displayed.")
  (on-render [this]
    "Runs each time the game is ready to render another frame.")
  (on-event [this event]
    "Runs each time an event you subscribed to fires."))

(defprotocol Game
  "A game object contains the internal renderer object and various bits of state
that are important to the overall execution of the game. Every play-cljs game
should create just one such object by calling [create-game](#create-game)."
  (start [game events]
    "Creates the canvas element and begins listening to the supplied events.")
  (stop [game]
    "Stops displaying any screen or listening to any events.")
  (render [game content]
    "Renders the provided data structure.")
  (pre-render [game width height content]
    "Renders the provided data structure off-screen and returns an [Image](#image) object.")
  (load-image [game path]
    "Returns an [Image](#image) object downloaded from the provided path.")
  (load-tiled-map [game map-name]
    "Returns a [TiledMap](#tiled-map) object. A tiled map with the provided name
must already be loaded (see the TiledMap docs for details).")
  (get-screens [game]
    "Returns a vector of the [Screen](#screen) objects currently being displayed.")
  (set-screens [game screens]
    "Sets new [Screen](#screen) objects to be displayed.")
  (get-screen [game]
    "Returns a single [Screen](#screen) object currently being displayed.")
  (set-screen [game screen]
    "Sets a new [Screen](#screen) object to be displayed.")
  (get-renderer [game]
    "Returns the internal renderer object.")
  (get-canvas [game]
    "Returns the internal canvas object.")
  (get-total-time [game]
    "Returns the total time transpired since the game started, in milliseconds.")
  (get-delta-time [game]
    "Returns the time since the last frame was rendered, in milliseconds.")
  (get-pressed-keys [game]
    "Returns a set containing the key codes for the keys currently being pressed.")
  (get-width [game]
    "Returns the virtual width of the game.")
  (get-height [game]
    "Returns the virtual height of the game.")
  (set-size [game width height]
    "Sets the virtual width and height of the game."))

(defn create-game
  "Returns a game object."
  [width height]
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
        (s/draw-sketch! renderer content {}))
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

