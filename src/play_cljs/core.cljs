(ns play-cljs.core
  (:require [goog.events :as events])
  (:require-macros [play-cljs.core :refer [run-on-all-screens!]]))

(defprotocol Screen
  (on-show [this state])
  (on-hide [this state])
  (on-render [this state timestamp])
  (on-keydown [this state event])
  (on-keyup [this state event]))

(defprotocol Game
  (start [this])
  (stop [this])
  (get-screens [this])
  (set-screens [this screens])
  (get-state [this])
  (set-state [this state]))

(defn process-commands! [game commands]
  (doseq [cmd (flatten commands)]
    (cond
      (map? cmd) (set-state game cmd)
      (fn? cmd) (some->> (cmd (get-state game)) (set-state game))
      (nil? cmd) nil
      :else (throw (js/Error. (str "Invalid command: " (pr-str cmd)))))))

(defn create-game [initial-state]
  (let [state-atom (atom initial-state)
        hidden-state-atom (atom {:screens []})]
    (reify Game
      (start [this]
        (->> (fn callback [timestamp]
               (run-on-all-screens! this on-render timestamp)
               (.requestAnimationFrame js/window callback))
             (.requestAnimationFrame js/window)
             (swap! hidden-state-atom assoc :request-id))
        (doto js/window
          (events/listen "keydown" #(run-on-all-screens! this on-keydown %))
          (events/listen "keyup" #(run-on-all-screens! this on-keyup %))))
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
        (reset! state-atom state)))))

