(ns play-cljs.utils
  (:require [clojure.spec.alpha :as s]))

(defn update-opts [opts parent-opts defaults]
  (let [parent-opts (merge defaults parent-opts)]
    (-> (merge defaults (dissoc parent-opts :x :y) opts)
        (update :x + (:x parent-opts))
        (update :y + (:y parent-opts)))))

(s/def ::halign #{:left :center :right})
(defn halign->constant [^js/p5 renderer halign]
  (get {:left (.-LEFT renderer)
        :center (.-CENTER renderer)
        :right (.-RIGHT renderer)}
    halign))

(s/def ::valign #{:top :center :bottom :baseline})
(defn valign->constant [^js/p5 renderer valign]
  (get {:top (.-TOP renderer)
        :center (.-CENTER renderer)
        :bottom (.-BOTTOM renderer)
        :baseline (.-BASELINE renderer)}
    valign))

(s/def ::style #{:normal :italic :bold})
(defn style->constant [^js/p5 renderer style]
  (get {:normal (.-NORMAL renderer)
        :italic (.-ITALIC renderer)
        :bold (.-BOLD renderer)}
    style))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::width number?)
(s/def ::height number?)

(s/def ::basic-opts (s/keys :opt-un [::x ::y ::width ::height]))
(def ^:const basic-defaults {:x 0 :y 0})

(s/def ::size number?)
(s/def ::font string?)
(s/def ::leading number?)

(s/def ::text-opts (s/keys :opt-un [::size ::font ::halign ::valign ::leading ::style]))
(def ^:const text-defaults (merge basic-defaults
                             {:size 32
                              :font "Helvetica"
                              :halign :left
                              :valign :baseline
                              :leading 0
                              :style :normal}))

(s/def ::name string?)
(s/def ::scale-x number?)
(s/def ::scale-y number?)
(s/def ::sx number?)
(s/def ::sy number?)

(s/def ::image-opts (s/keys
                      :req-un [::name]
                      :opt-un [::scale-x ::scale-y ::sx ::sy]))
(def ^:const img-defaults (merge basic-defaults {:scale-x 1 :scale-y 1 :sx 0 :sy 0}))

(s/def ::max-red #(<= 0 % 255))
(s/def ::max-green #(<= 0 % 255))
(s/def ::max-blue #(<= 0 % 255))

(s/def ::max-hue #(<= 0 % 360))
(s/def ::max-saturation #(<= 0 % 100))
(s/def ::max-brightness #(<= 0 % 100))

(s/def ::max-alpha #(<= 0 % 255))

(s/def ::rgb-opts (s/keys :opt-un [::max-red ::max-green ::max-blue ::max-alpha]))
(def ^:const rgb-defaults (merge basic-defaults {:max-red 255 :max-green 255 :max-blue 255 :max-alpha 1}))

(s/def ::hsb-opts (s/keys :opt-un [::max-hue ::max-saturation ::max-brightness ::max-alpha]))
(def ^:const hsb-defaults (merge basic-defaults {:max-hue 360 :max-saturation 100 :max-brightness 100 :max-alpha 1}))

