(ns play-cljs.utils
  (:require [clojure.string :as str]))

(defn ^:private split-key [k]
  (-> k name (str/split #"-")))

(defn key->camel [k]
  (let [parts (split-key k)]
    (->> (rest parts)
         (map str/capitalize)
         (cons (first parts))
         (str/join ""))))

