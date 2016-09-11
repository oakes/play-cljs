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

(def core (or (->> (read-namespaces "../src/play_cljs")
                   (filter #(= (:name %) 'play-cljs.core))
                   first
                   :publics)
              (throw (Exception. "Couldn't find the namespace"))))

(def create-game (or (first (filter #(= (:name %) 'create-game) core))
                     (throw (Exception. "Couldn't find create-game"))))

(def game (or (-> (filter #(= (:name %) 'Game) core)
                  first
                  (update :members
                    (fn [members]
                      (->> members
                           (sort-by :name)
                           (cons create-game)))))
              (throw (Exception. "Couldn't find Game"))))

(def screen (or (-> (filter #(= (:name %) 'Screen) core)
                    first)
                (throw (Exception. "Couldn't find Screen"))))

(defn parse-file [fname]
  (-> fname slurp edn/read-string))

(defn item->str [{:keys [name arglists doc members sub-item? example]}]
  (list
    [:a {:name (str/replace (str name) #"\." "")}]
    (cond
      (not sub-item?)
      [:h1 (str name)]
      (empty? arglists)
      [:h2 (str name)]
      :else
      (for [arglist arglists]
        [:h2 (pr-str (cons (symbol name) (apply list arglist)))]))
    (md-to-html-string doc)
    (when example
      [:div {:class "paren-soup"}
       [:div {:class "content" :contenteditable "true"}
        example]])
    (map #(-> % (assoc :sub-item? true) item->str) members)))

(spit "build/index.html"
  (html [:html
         [:head
          [:link {:rel "stylesheet" :type "text/css" :href "paren-soup-light.css"}]]
         [:body
          (item->str (dissoc screen :members))
          (item->str game)
          (item->str (parse-file "elements.edn"))
          (item->str (parse-file "image.edn"))
          (item->str (parse-file "tiled-map.edn"))
          (item->str (parse-file "vector.edn"))
          (item->str (parse-file "color.edn"))
          [:script {:src "paren-soup.js" :type "text/javascript"}]
          [:script {:type "text/javascript"}
           "paren_soup.core.init_all();"]]]))

