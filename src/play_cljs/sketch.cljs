(ns play-cljs.sketch
  (:require [p5.core]))

(defn update-opts [opts parent-opts defaults]
  (let [parent-opts (merge defaults parent-opts)]
    (-> (merge defaults (dissoc parent-opts :x :y) opts)
        (update :x + (:x parent-opts))
        (update :y + (:y parent-opts)))))

(def ^:const basic-defaults {:x 0 :y 0 :scale-x 1 :scale-y 1})
(def ^:const text-defaults (merge basic-defaults {:size 32 :font "Helvetica" :halign :left :valign :baseline :leading 0 :style :normal}))
(def ^:const img-defaults (merge basic-defaults {:sx 0 :sy 0}))

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

(defmethod draw-sketch! :div [renderer content parent-opts]
  (let [[command opts & children] content
        opts (update-opts opts parent-opts basic-defaults)]
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :text [renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [x y size font halign valign leading style] :as opts}
        (update-opts opts parent-opts text-defaults)]
    (doto renderer
      (.textSize size)
      (.textFont font)
      (.textAlign (halign->constant renderer halign) (valign->constant renderer valign))
      (.textLeading leading)
      (.textStyle (style->constant renderer style))
      (.text command x y))
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :arc [renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [x y width height start stop] :as opts}
        (update-opts opts parent-opts basic-defaults)]
    (.arc renderer x y width height start stop)
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :ellipse [renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [x y width height] :as opts}
        (update-opts opts parent-opts basic-defaults)]
    (.ellipse renderer x y width height)
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :line [renderer content parent-opts]
  (let [[command opts & children] content
        opts (update-opts opts parent-opts basic-defaults)
        {:keys [x1 y1 x2 y2] :as opts}
        (-> opts
            (update :x1 + (:x opts))
            (update :y1 + (:y opts))
            (update :x2 + (:x opts))
            (update :y2 + (:y opts)))]
    (.line renderer x1 y1 x2 y2)
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :point [renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [x y] :as opts}
        (update-opts opts parent-opts basic-defaults)]
    (.point renderer x y)
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :quad [renderer content parent-opts]
  (let [[command opts & children] content
        opts (update-opts opts parent-opts basic-defaults)
        {:keys [x1 y1 x2 y2 x3 y3 x4 y4] :as opts}
        (-> opts
            (update :x1 + (:x opts))
            (update :y1 + (:y opts))
            (update :x2 + (:x opts))
            (update :y2 + (:y opts))
            (update :x3 + (:x opts))
            (update :y3 + (:y opts))
            (update :x4 + (:x opts))
            (update :y4 + (:y opts)))]
    (.quad renderer x1 y1 x2 y2 x3 y3 x4 y4)
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :rect [renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [x y width height] :as opts}
        (update-opts opts parent-opts basic-defaults)]
    (.rect renderer x y width height)
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :triangle [renderer content parent-opts]
  (let [[command opts & children] content
        opts (update-opts opts parent-opts basic-defaults)
        {:keys [x1 y1 x2 y2 x3 y3] :as opts}
        (-> opts
            (update :x1 + (:x opts))
            (update :y1 + (:y opts))
            (update :x2 + (:x opts))
            (update :y2 + (:y opts))
            (update :x3 + (:x opts))
            (update :y3 + (:y opts)))]
    (.triangle renderer x1 y1 x2 y2 x3 y3)
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :img [renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [object x y width height sx sy swidth sheight scale-x scale-y] :as opts}
        (update-opts opts parent-opts img-defaults)
        swidth (or swidth (.-width object))
        sheight (or sheight (.-height object))]
    (.scale renderer scale-x scale-y)
    (.image renderer object
      sx sy swidth sheight
      x y (or width swidth) (or height sheight))
    (draw-sketch! renderer children opts)))

(defmethod draw-sketch! :fill [renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [grayscale rgb color] :as opts}
        (update-opts opts parent-opts basic-defaults)
        fill-fn (cond
                  grayscale
                  #(.fill renderer grayscale)
                  rgb
                  (let [[red green blue] rgb]
                    #(.fill renderer red green blue))
                  color
                  #(.fill renderer color)
                  :else
                  #(.noFill renderer))]
    (fill-fn)
    (draw-sketch! renderer children (assoc opts :fill-fn fill-fn))
    ; reset fill to its default
    (.noFill renderer)
    (set! (.-_fillSet (.-_renderer renderer)) false)
    ; if there is a fill function in a parent, re-apply it
    (when-let [fill-fn (:fill-fn parent-opts)]
      (fill-fn))))

(defmethod draw-sketch! :stroke [renderer content parent-opts]
  (let [[command opts & children] content
        {:keys [grayscale rgb color] :as opts}
        (update-opts opts parent-opts basic-defaults)
        stroke-fn (cond
                    grayscale
                    #(.stroke renderer grayscale)
                    rgb
                    (let [[red green blue] rgb]
                      #(.stroke renderer red green blue))
                    color
                    #(.stroke renderer color)
                    :else
                    #(.noStroke renderer))]
    (stroke-fn)
    (draw-sketch! renderer children (assoc opts :stroke-fn stroke-fn))
    ; reset stroke to its default
    (.stroke renderer "black")
    (set! (.-_strokeSet (.-_renderer renderer)) false)
    ; if there is a stroke function in a parent, re-apply it
    (when-let [stroke-fn (:stroke-fn parent-opts)]
      (stroke-fn))))

(defmethod draw-sketch! :default [renderer content parent-opts]
  (cond
    (sequential? (first content))
    (run! #(draw-sketch! renderer % parent-opts) content)
    (nil? (first content))
    nil
    :else
    (throw (js/Error. (str "Invalid sketch command: " (pr-str content))))))

