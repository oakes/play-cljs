(ns play-cljs.options
  (:require [clojure.spec.alpha :as s]
            [expound.alpha :as expound]))

(defn update-opts [opts parent-opts defaults]
  (let [parent-opts (merge defaults parent-opts)]
    (-> (merge defaults (dissoc parent-opts :x :y) opts)
        (update :x + (:x parent-opts))
        (update :y + (:y parent-opts)))))

(defn check-opts [spec opts]
  (when (= :cljs.spec.alpha/invalid (s/conform spec opts))
    (throw (js/Error. (expound/expound-str spec opts)))))

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

(s/def :play-cljs.options.text/value string?)
(s/def ::size number?)
(s/def ::font string?)
(s/def ::leading number?)

(s/def ::text-opts (s/merge
                     ::basic-opts
                     (s/keys
                       :req-un [:play-cljs.options.text/value]
                       :opt-un [::size ::font ::halign ::valign ::leading ::style])))
(def ^:const text-defaults (merge basic-defaults
                             {:size 32
                              :font "Helvetica"
                              :halign :left
                              :valign :baseline
                              :leading 0
                              :style :normal}))

(s/def ::start number?)
(s/def ::stop number?)

(s/def ::arc-opts (s/merge
                     ::basic-opts
                     (s/keys :req-un [::width ::height ::start ::stop])))

(s/def ::ellipse-opts (s/merge
                        ::basic-opts
                        (s/keys :req-un [::width ::height])))

(s/def ::x1 number?)
(s/def ::y1 number?)
(s/def ::x2 number?)
(s/def ::y2 number?)
(s/def ::x3 number?)
(s/def ::y3 number?)
(s/def ::x4 number?)
(s/def ::y4 number?)

(s/def ::line-opts (s/keys :req-un [::x1 ::y1 ::x2 ::y2]))

(s/def ::point-opts (s/keys :req-un [::x ::y]))

(s/def ::quad-opts (s/keys :req-un [::x1 ::y1 ::x2 ::y2 ::x3 ::y3 ::x4 ::y4]))

(s/def ::rect-opts (s/merge
                     ::basic-opts
                     (s/keys :req-un [::width ::height])))

(s/def ::triangle-opts (s/keys :req-un [::x1 ::y1 ::x2 ::y2 ::x3 ::y3]))

(s/def :play-cljs.options.image/value #(instance? js/p5.Image %))
(s/def ::name string?)
(s/def ::scale-x number?)
(s/def ::scale-y number?)
(s/def ::sx number?)
(s/def ::sy number?)
(s/def ::swidth number?)
(s/def ::sheight number?)
(s/def ::flip-x boolean?)
(s/def ::flip-y boolean?)

(s/def ::image-opts (s/merge
                      ::basic-opts
                      (s/keys
                        :req-un [(or ::name :play-cljs.options.image/value)]
                        :opt-un [::scale-x ::scale-y ::sx ::sy ::swidth ::sheight ::flip-x ::flip-y])))
(def ^:const image-defaults (merge basic-defaults {:scale-x 1 :scale-y 1 :sx 0 :sy 0}))

(s/def ::duration number?)

(s/def ::animation-opts (s/merge
                          ::basic-opts
                          (s/keys :req-un [::duration])))

(s/def ::grayscale #(<= 0 % 255))
(s/def ::color string?)
(s/def ::colors (s/coll-of number?))

(s/def ::fill-opts (s/keys :opt-un [::grayscale ::color ::colors]))

(s/def ::stroke-opts (s/keys :opt-un [::grayscale ::color ::colors]))

(s/def ::z1 number?)
(s/def ::z2 number?)
(s/def ::z3 number?)
(s/def ::z4 number?)

(s/def ::bezier-opts (s/keys
                       :req-un [::x1 ::y1 ::x2 ::y2 ::x3 ::y3 ::x4 ::y4]
                       :opt-un [::z1 ::z2 ::z3 ::z4]))

(s/def ::curve-opts (s/keys
                      :req-un [::x1 ::y1 ::x2 ::y2 ::x3 ::y3 ::x4 ::y4]
                      :opt-un [::z1 ::z2 ::z3 ::z4]))

(s/def :play-cljs.options.rgb/max-r #(<= 0 % 255))
(s/def :play-cljs.options.rgb/max-g #(<= 0 % 255))
(s/def :play-cljs.options.rgb/max-b #(<= 0 % 255))
(s/def :play-cljs.options.rgb/max-a #(<= 0 % 255))

(s/def ::rgb-opts (s/keys
                    :req-un [:play-cljs.options.rgb/max-r
                             :play-cljs.options.rgb/max-g
                             :play-cljs.options.rgb/max-b]
                    :opt-un [:play-cljs.options.rgb/max-a]))

(s/def :play-cljs.options.hsb/max-h #(<= 0 % 360))
(s/def :play-cljs.options.hsb/max-s #(<= 0 % 100))
(s/def :play-cljs.options.hsb/max-b #(<= 0 % 100))
(s/def :play-cljs.options.hsb/max-a #(<= 0 % 255))

(s/def ::hsb-opts (s/keys
                    :req-un [:play-cljs.options.hsb/max-h
                             :play-cljs.options.hsb/max-s
                             :play-cljs.options.hsb/max-b]
                    :opt-un [:play-cljs.options.hsb/max-a]))

(s/def :play-cljs.options.hsl/max-h #(<= 0 % 360))
(s/def :play-cljs.options.hsl/max-s #(<= 0 % 100))
(s/def :play-cljs.options.hsl/max-l #(<= 0 % 100))
(s/def :play-cljs.options.hsl/max-a #(<= 0 % 255))

(s/def ::hsl-opts (s/keys
                    :req-un [:play-cljs.options.hsl/max-h
                             :play-cljs.options.hsl/max-s
                             :play-cljs.options.hsl/max-l]
                    :opt-un [:play-cljs.options.hsl/max-a]))

(s/def :play-cljs.options.tiled-map/value #(instance? js/p5.TiledMap %))

(s/def ::tiled-map-opts (s/merge
                          ::basic-opts
                          (s/keys :req-un [(or ::name :play-cljs.options.tiled-map/value)])))

(s/def ::points (s/and (s/coll-of number?) #(even? (count %))))

(s/def ::shape-opts (s/keys :req-un [::points]))

(s/def ::contour-opts (s/keys :req-un [::points]))

