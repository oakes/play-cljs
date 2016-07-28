(ns play-cljs.core
  (:require [goog.events :as events]
            [cljsjs.pixi]
            [play-cljs.graphics :as g]
            [play-cljs.utils :as u])
  (:require-macros [play-cljs.core :refer [run-on-all-screens!]]))

(defprotocol Screen
  (on-show [this state])
  (on-hide [this state])
  (on-render [this state total-time delta-time])
  (on-event [this state event]))

(defprotocol Game
  (start [this events])
  (stop [this])
  (get-screens [this])
  (set-screens [this screens])
  (get-state [this])
  (set-state [this state])
  (get-renderer [this])
  (set-renderer [this renderer])
  (get-pressed-key [this])
  (key-pressed? [this key-name]))

(defprotocol Command
  (run [this game]))

(defn process-command! [game cmd]
  (cond
    (sequential? cmd) (run! #(process-command! game %) cmd)
    (satisfies? Command cmd) (run cmd game)
    (fn? cmd) (process-command! (cmd (get-state game)))
    (nil? cmd) nil
    :else (throw (js/Error. (str "Invalid command: " (pr-str cmd))))))

(defn create-renderer [width height opts]
  (let [opts (->> opts
                  (map (fn [[k v]] [(u/key->camel k) v]))
                  (into {}))]
    (.autoDetectRenderer js/PIXI width height (clj->js opts))))

(defn create-game [renderer]
  (let [state-atom (atom {})
        hidden-state-atom (atom {:screens [] :renderer renderer :time 0 :last-key nil})]
    (reify Game
      (start [this events]
        (->> (fn callback [time]
               (run-on-all-screens! this on-render time (- time (:time @hidden-state-atom)))
               (swap! hidden-state-atom assoc :time time)
               (.requestAnimationFrame js/window callback))
             (.requestAnimationFrame js/window)
             (swap! hidden-state-atom assoc :request-id))
        (doto js/window
          (events/listen "keydown" #(swap! hidden-state-atom assoc :last-key %))
          (events/listen "keyup" #(swap! hidden-state-atom dissoc :last-key %)))
        (doseq [event events]
          (events/listen js/window event #(run-on-all-screens! this on-event %))))
      (stop [this]
        (.cancelAnimationFrame js/window (:request-id @hidden-state-atom))
        (events/removeAll js/window))
      (get-screens [this]
        (:screens @hidden-state-atom))
      (set-screens [this screens]
        (run-on-all-screens! this on-hide)
        (swap! hidden-state-atom assoc :screens screens)
        (run-on-all-screens! this on-show))
      (get-state [this]
        @state-atom)
      (set-state [this state]
        (reset! state-atom state)
        state)
      (get-renderer [this]
        (:renderer @hidden-state-atom))
      (set-renderer [this renderer]
        (swap! hidden-state-atom assoc :renderer renderer)
        renderer)
      (get-pressed-key [this]
        (:last-key @hidden-state-atom))
      (key-pressed? [this key-name]
        (some-> (get-pressed-key this) .-event_ .-key (= (u/key->pascal key-name)))))))

(defrecord ResetState [state] Command
  (run [{:keys [state]} game]
    (set-state game state)))

(defn reset-state [state]
  (ResetState. state))

(defrecord Graphics [command x y] Command
  (run [{:keys [command x y]} game]
    (let [renderer (get-renderer game)
          graphics (js/PIXI.Graphics.)]
      (g/draw-graphics! command x y graphics)
      (.render renderer graphics))))

(defn graphics
  ([command]
   (graphics command 0 0))
  ([command x y]
   (Graphics. command x y)))

(defrecord Sprite [url x y frame scale anchor width height] Command
  (run [{:keys [url x y frame scale anchor width height]} game]
    (let [texture (.fromImage js/PIXI.Texture url)
          texture (if frame
                    (doto (.clone texture)
                      (#(set! (.-frame %) frame)))
                    texture)
          sprite (js/PIXI.Sprite. texture)
          renderer (get-renderer game)]
      (.set (.-position sprite) x y)
      (when-let [[x y] anchor]
        (.set (.-anchor sprite) x y))
      (when-let [[x y] scale]
        (.set (.-scale sprite) x y))
      (some->> width (set! (.-width sprite)))
      (some->> height (set! (.-height sprite)))
      (.render renderer sprite))))

(defn sprite
  ([url]
   (sprite url 0 0))
  ([url x y]
   (sprite url x y nil))
  ([url x y opts]
   (let [{:keys [frame scale anchor width height]} opts]
     (Sprite. url x y frame scale anchor width height))))

(defn rectangle [x y width height]
  (js/PIXI.Rectangle. x y width height))

