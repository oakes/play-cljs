#!/usr/bin/env boot

(set-env! :dependencies '[[org.clojure/core.async "0.2.385"]
                          [codox "0.9.7"]
                          [hiccup "1.0.5"]
                          [markdown-clj "0.9.89"]])
(require
  '[clojure.string :as str]
  '[clojure.edn :as edn]
  '[codox.reader.clojurescript :refer [read-namespaces]]
  '[hiccup.core :refer [html]]
  '[markdown.core :refer [md-to-html-string]])

(def core (or (->> (read-namespaces "src/play_cljs")
                   (filter #(= (:name %) 'play-cljs.core))
                   first
                   :publics)
              (throw (Exception. "Couldn't find the namespace"))))

; Game

(def game (or (->> core
                   (filter #(= (:name %) 'Game))
                   first)
              (throw (Exception. "Couldn't find Game"))))

; Screen

(def screen (or (->> core
                     (filter #(= (:name %) 'Screen))
                     first)
                (throw (Exception. "Couldn't find Screen"))))

; Functions

(def create-game (or (->> core
                          (filter #(= (:name %) 'create-game))
                          first)
                     (throw (Exception. "Couldn't find create-game"))))

(def game-fns (->> game :members (sort-by :name)))

; Elements

(def elements [{:name :div}
               {:name :text}
               {:name :arc}
               {:name :ellipse}
               {:name :line}
               {:name :point}
               {:name :quad}
               {:name :rect}
               {:name :triangle}
               {:name :image}
               {:name :fill}
               {:name :stroke}
               {:name :bezier}
               {:name :curve}
               {:name :rgb}
               {:name :hsb}
               {:name :tiled-map}])

(defn parse-file [fname]
  (-> fname slurp edn/read-string))

(defn description->str [{:keys [name arglists doc members]}]
  (list
    [:a {:name (str/replace (str name) #"\." "")}]
    (for [arglist arglists]
      [:h2 (pr-str (conj (apply list arglist) (symbol name)))])
    (md-to-html-string doc)
    (map description->str members)))

(spit "doc.html"
  (html [:html
         [:body
          [:h1 "Screen"]
          (description->str (dissoc screen :members))
          [:h1 "Game"]
          (description->str (dissoc game :members))
          [:h1 "Functions"]
          (description->str create-game)
          (map description->str game-fns)
          [:h1 "Elements"]
          [:h2 "Coming soon..."]
          [:h1 "Image"]
          (description->str (parse-file "doc/image.edn"))
          [:h1 "TiledMap"]
          (description->str (parse-file "doc/tiled-map.edn"))
          [:h1 "Vector"]
          (description->str (parse-file "doc/vector.edn"))
          [:h1 "Color"]
          (description->str (parse-file "doc/color.edn"))]]))

