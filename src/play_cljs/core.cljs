(ns play-cljs.core
  (:require [goog.events :as events]
            [cljsjs.pixi]
            [play-cljs.graphics :as g]
            [play-cljs.utils :as u]))

(defprotocol Screen
  (on-show [this state])
  (on-hide [this state])
  (on-render [this state])
  (on-event [this state event]))

(defprotocol Game
  (start [this events])
  (stop [this])
  (get-screens [this])
  (set-screens [this screens])
  (get-screen [this])
  (set-screen [this screen])
  (get-state [this])
  (set-state [this state])
  (get-renderer [this])
  (set-renderer [this renderer])
  (get-total-time [this])
  (get-delta-time [this])
  (get-pressed-keys [this])
  (get-width [this])
  (get-height [this]))

(defprotocol Command
  (run [this game]))

(defn ^:private process-command! [game cmd]
  (cond
    (sequential? cmd) (run! #(process-command! game %) cmd)
    (satisfies? Command cmd) (run cmd game)
    (nil? cmd) nil
    :else (throw (js/Error. (str "Invalid command: " (pr-str cmd))))))

(defn ^:private run-on-all-screens! [game f]
  (run! #(process-command! game (f % (get-state game)))
    (get-screens game)))

(defn create-renderer [width height opts]
  (let [opts (->> opts
                  (merge {:clear-before-render false})
                  (map (fn [[k v]] [(u/key->camel k) v]))
                  (into {}))]
    (.autoDetectRenderer js/PIXI width height (clj->js opts))))

(defn create-game [renderer]
  (let [state-atom (atom {})
        hidden-state-atom (atom {:screens []
                                 :renderer renderer
                                 :total-time 0
                                 :delta-time 0
                                 :pressed-keys #{}})]
    (reify Game
      (start [this events]
        (->> (fn callback [time]
               (swap! hidden-state-atom
                 (fn [hidden-state]
                   (assoc hidden-state
                     :total-time time
                     :delta-time (- time (:total-time hidden-state)))))
               (run-on-all-screens! this on-render)
               (.requestAnimationFrame js/window callback))
             (.requestAnimationFrame js/window)
             (swap! hidden-state-atom assoc :request-id))
        (doto js/window
          (events/listen "keydown" #(swap! hidden-state-atom update :pressed-keys conj (.-keyCode %)))
          (events/listen "keyup" #(if (contains? #{91 93} (.-keyCode %))
                                    (swap! hidden-state-atom assoc :pressed-keys #{})
                                    (swap! hidden-state-atom update :pressed-keys disj (.-keyCode %))))
          (events/listen "blur" #(swap! hidden-state-atom assoc :pressed-keys #{})))
        (doseq [event events]
          (events/listen js/window event (fn [e]
                                           (run-on-all-screens! this (fn [screen state]
                                                                       (on-event screen state e)))))))
      (stop [this]
        (.cancelAnimationFrame js/window (:request-id @hidden-state-atom))
        (events/removeAll js/window))
      (get-screens [this]
        (:screens @hidden-state-atom))
      (set-screens [this screens]
        (run-on-all-screens! this on-hide)
        (swap! hidden-state-atom assoc :screens screens)
        (run-on-all-screens! this on-show))
      (get-screen [this]
        (first (get-screens this)))
      (set-screen [this screen]
        (set-screens this [screen]))
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
      (get-total-time [this]
        (:total-time @hidden-state-atom))
      (get-delta-time [this]
        (:delta-time @hidden-state-atom))
      (get-pressed-keys [this]
        (:pressed-keys @hidden-state-atom))
      (get-width [this]
        (-> (get-renderer this) .-view .-width))
      (get-height [this]
        (-> (get-renderer this) .-view .-height)))))

(defrecord DelayCommand [function] Command
  (run [{:keys [function]} game]
    (process-command! game (function (get-state game)))))

(defn delay-command [function]
  (DelayCommand. function))

(defrecord ResetState [state] Command
  (run [{:keys [state]} game]
    (set-state game state)))

(defn reset-state [state]
  (ResetState. state))

(defrecord Graphics [object content x y] Command
  (run [{:keys [object content x y]} game]
    (let [renderer (get-renderer game)]
      (.clear object)
      (g/draw-graphics! object content x y)
      (.render renderer object))))

(defn graphics
  ([content]
   (graphics content nil))
  ([content opts]
   (let [{:keys [x y]
          :or {x 0 y 0}} opts
         object (js/PIXI.Graphics.)]
     (Graphics. object content x y))))

(defrecord Sprite [object x y width height] Command
  (run [{:keys [object x y width height]} game]
    (.set (.-position object) x y)
    (some->> width (set! (.-width object)))
    (some->> height (set! (.-height object)))
    (.render (get-renderer game) object)))

(defn sprite
  ([url]
   (sprite url nil))
  ([url opts]
   (let [{:keys [x y width height frame anchor scale]
          :or {x 0 y 0}} opts
         texture (.fromImage js/PIXI.Texture url)
         texture (if frame
                   (let [texture (.clone texture)
                         {:keys [x y width height]} frame
                         rect (js/PIXI.Rectangle. x y width height)]
                     (set! (.-frame texture) rect)
                     texture)
                   texture)
         object (js/PIXI.Sprite. texture)]
     (when-let [[x y] anchor]
       (.set (.-anchor object) x y))
     (when-let [[x y] scale]
       (.set (.-scale object) x y))
     (Sprite. object x y width height))))

(defrecord MovieClip [object sprites x y width height animation-speed play? loop?] Command
  (run [{:keys [object sprites x y width height animation-speed play? loop?]} game]
    (.set (.-position object) x y)
    (some->> width (set! (.-width object)))
    (some->> height (set! (.-height object)))
    (let [sprite (get sprites (.-currentFrame object))]
      (set! (.-anchor object) (.-anchor sprite))
      (set! (.-scale object) (.-scale sprite)))
    (set! (.-animationSpeed object) animation-speed)
    (if play?
      (when-not (.-playing object) (.play object))
      (when (.-playing object) (.stop object)))
    (set! (.-loop object) loop?)
    (.render (get-renderer game) object)))

(defn movie-clip
  ([sprites]
   (movie-clip sprites nil))
  ([sprites opts]
   (let [{:keys [x y width height animation-speed play? loop?]
          :or {x 0 y 0 play? true loop? true}} opts
         sprites (mapv :object sprites)
         textures (mapv #(.-texture %) sprites)
         object (js/PIXI.extras.MovieClip. (into-array textures))]
     (MovieClip. object sprites x y width height animation-speed play? loop?))))

(defrecord Text [object x y width height] Command
  (run [{:keys [object x y width height]} game]
    (.set (.-position object) x y)
    (some->> width (set! (.-width object)))
    (some->> height (set! (.-height object)))
    (.render (get-renderer game) object)))

(defn text
  ([text-str]
   (text text-str nil))
  ([text-str opts]
   (let [{:keys [x y width height]
          :or {x 0 y 0}} opts
         opts (->> opts
                   (map (fn [[k v]] [(u/key->camel k) v]))
                   (into {}))]
     (Text. (js/PIXI.Text. text-str (clj->js opts)) x y width height))))

