(ns play-cljs.sketch
  (:require [p5.core]))

(defn update-opts [opts parent-opts defaults]
  (let [parent-opts (merge defaults parent-opts)]
    (-> (merge defaults (dissoc parent-opts :x :y) opts)
        (update :x + (:x parent-opts))
        (update :y + (:y parent-opts)))))

(def ^:const basic-defaults {:x 0 :y 0})
(def ^:const text-defaults (merge basic-defaults {:size 32 :font "Helvetica" :halign :left :valign :baseline :leading 0 :style :normal}))

(defn halign->constant [renderer halign]
  (get {:left (.-LEFT renderer) :center (.-CENTER renderer) :right (.-RIGHT renderer)} halign))

(defn valign->constant [renderer valign]
  (get {:top (.-TOP renderer) :center (.-CENTER renderer) :bottom (.-BOTTOM renderer) :baseline (.-BASELINE renderer)} valign))

(defn style->constant [renderer style]
  (get {:normal (.-NORMAL renderer) :italic (.-ITALIC renderer) :bold (.-BOLD renderer)} style))

(defmulti draw-sketch! (fn [renderer content parent-opts]
                         (let [command (first content)]
                           (if (string? command)
                             :text
                             command))))

(defmethod draw-sketch! :text [renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [x y size font halign valign leading style] :as opts} (update-opts opts parent-opts text-defaults)]
    (.textSize renderer size)
    (.textFont renderer font)
    (.textAlign renderer (halign->constant renderer halign) (valign->constant renderer valign))
    (.textLeading renderer leading)
    (.textStyle renderer (style->constant renderer style))
    (.text renderer command x y)
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :arc [renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [x y width height start stop] :as opts} (update-opts opts parent-opts basic-defaults)]
    (.arc renderer x y width height start stop)
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :default [renderer content parent-opts]
  (cond
    (sequential? (first content))
    (run! #(draw-sketch! renderer % parent-opts) content)
    (nil? (first content))
    nil
    :else
    (throw (js/Error. (str "Invalid sketch command: " (pr-str content))))))

