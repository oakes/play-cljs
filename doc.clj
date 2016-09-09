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

(def game (or (-> (filter #(= (:name %) 'Game) core)
                  first
                  (assoc :top-level? true))
              (throw (Exception. "Couldn't find Game"))))

(def screen (or (-> (filter #(= (:name %) 'Screen) core)
                    first
                    (assoc :top-level? true))
                (throw (Exception. "Couldn't find Screen"))))

(def create-game (or (first (filter #(= (:name %) 'create-game) core))
                     (throw (Exception. "Couldn't find create-game"))))

(def game-fns (->> game :members (sort-by :name)))

(defn parse-file [fname]
  (-> fname slurp edn/read-string))

(defn item->str [{:keys [name arglists doc members top-level? example]}]
  (list
    [:a {:name (str/replace (str name) #"\." "")}]
    (cond
      top-level?
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
    (map item->str members)))

(spit "doc.html"
  (html [:html
         [:head
          [:link {:rel "stylesheet" :type "text/css" :href "paren-soup-light.css"}]]
         [:body
          (item->str (dissoc screen :members))
          (item->str (dissoc game :members))
          (item->str create-game)
          (map item->str game-fns)
          (item->str (parse-file "doc/elements.edn"))
          (item->str (parse-file "doc/image.edn"))
          (item->str (parse-file "doc/tiled-map.edn"))
          (item->str (parse-file "doc/vector.edn"))
          (item->str (parse-file "doc/color.edn"))
          [:script {:src "paren-soup.js" :type "text/javascript"}]
          [:script {:type "text/javascript"}
           "paren_soup.core.init_all();"]]]))

