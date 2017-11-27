(ns play-cljs.utils)

(defn update-opts [opts parent-opts defaults]
  (let [parent-opts (merge defaults parent-opts)]
    (-> (merge defaults (dissoc parent-opts :x :y) opts)
        (update :x + (:x parent-opts))
        (update :y + (:y parent-opts)))))

(def ^:const basic-defaults {:x 0 :y 0 :scale-x 1 :scale-y 1})
(def ^:const text-defaults (merge basic-defaults {:size 32 :font "Helvetica" :halign :left :valign :baseline :leading 0 :style :normal}))
(def ^:const img-defaults (merge basic-defaults {:sx 0 :sy 0}))
(def ^:const rgb-defaults (merge basic-defaults {:max-r 255 :max-g 255 :max-b 255 :max-a 1}))
(def ^:const hsb-defaults (merge basic-defaults {:max-h 360 :max-s 100 :max-b 100 :max-a 1}))

(defn halign->constant [^js/p5 renderer halign]
  (get {:left (.-LEFT renderer) :center (.-CENTER renderer) :right (.-RIGHT renderer)} halign))

(defn valign->constant [^js/p5 renderer valign]
  (get {:top (.-TOP renderer) :center (.-CENTER renderer) :bottom (.-BOTTOM renderer) :baseline (.-BASELINE renderer)} valign))

(defn style->constant [^js/p5 renderer style]
  (get {:normal (.-NORMAL renderer) :italic (.-ITALIC renderer) :bold (.-BOLD renderer)} style))

