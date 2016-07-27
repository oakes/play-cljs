(ns play-cljs.core
  (:require [goog.events :as events]
            [cljsjs.pixi]
            [play-cljs.utils :as u])
  (:require-macros [play-cljs.core :refer [run-on-all-screens!]]))

(defprotocol Screen
  (on-show [this state])
  (on-hide [this state])
  (on-render [this state timestamp])
  (on-event [this state event]))

(defprotocol Game
  (start [this events])
  (stop [this])
  (get-screens [this])
  (set-screens [this screens])
  (get-state [this])
  (set-state [this state])
  (get-renderer [this]))

(defprotocol Command
  (run [this game]))

(defn process-commands! [game commands]
  (doseq [cmd commands]
    (cond
      (satisfies? Command cmd) (run cmd game)
      (map? cmd) (set-state game cmd)
      (fn? cmd) (when-let [result (cmd (get-state game))]
                  (assert (map? result) "Result of function should be nil or a state map.")
                  (set-state game result))
      (nil? cmd) nil
      (sequential? cmd) (process-commands! game cmd)
      :else (throw (js/Error. (str "Invalid command: " (pr-str cmd)))))))

(defn create-renderer [width height opts]
  (let [opts (->> opts
                  (map (fn [[k v]] [(u/key->camel k) v]))
                  (into {}))]
    (.autoDetectRenderer js/PIXI width height (clj->js opts))))

(defn create-game [renderer initial-state]
  (let [state-atom (atom initial-state)
        hidden-state-atom (atom {:screens []})]
    (reify Game
      (start [this events]
        (->> (fn callback [timestamp]
               (run-on-all-screens! this on-render timestamp)
               (.requestAnimationFrame js/window callback))
             (.requestAnimationFrame js/window)
             (swap! hidden-state-atom assoc :request-id))
        (doseq [event events]
          (events/listen (.-view renderer) event #(run-on-all-screens! this on-event %))))
      (stop [this]
        (.cancelAnimationFrame (.-view renderer) (:request-id @hidden-state-atom))
        (events/removeAll (.-view renderer)))
      (get-screens [this]
        (:screens @hidden-state-atom))
      (set-screens [this screens]
        (run-on-all-screens! this on-hide)
        (swap! hidden-state-atom assoc :screens screens)
        (run-on-all-screens! this on-show))
      (get-state [this]
        @state-atom)
      (set-state [this state]
        (reset! state-atom state))
      (get-renderer [this]
        renderer))))

