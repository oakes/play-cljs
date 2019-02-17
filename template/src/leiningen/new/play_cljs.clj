(ns leiningen.new.play-cljs
  (:require [leiningen.new.templates :as t]
            [clojure.string :as str]))

(defn sanitize-name [s]
  (as-> s $
        (str/trim $)
        (str/lower-case $)
        (str/replace $ "'" "")
        (str/replace $ #"[^a-z0-9]" " ")
        (str/split $ #" ")
        (remove empty? $)
        (str/join "-" $)))

(defn play-cljs
  [name & _]
  (let [render (t/renderer "play-cljs")
        [project-name core-name] (str/split name #"\." 2)
        project-name (sanitize-name project-name)
        core-name (if core-name (sanitize-name core-name) "core")
        _ (when (or (not (seq project-name))
                    (not (seq core-name)))
            (throw (Exception. (str "Invalid name: " name))))
        data {:name project-name
              :core-name core-name
              :project_name (str/replace project-name "-" "_")
              :core_name (str/replace core-name "-" "_")}]
    (t/->files data
      ["README.md" (render "README.md" data)]
      [".gitignore" (render "gitignore" data)]
      ["deps.edn" (render "deps.edn" data)]
      ["figwheel-main.edn" (render "figwheel-main.edn" data)]
      ["dev.cljs.edn" (render "dev.cljs.edn" data)]
      ["dev.clj" (render "dev.clj" data)]
      ["prod.clj" (render "prod.clj" data)]
      ["src/{{project_name}}/{{core_name}}.cljs" (render "core.cljs" data)]
      ["src/{{project_name}}/music.clj" (render "music.clj" data)]
      ["src/{{project_name}}/dev.cljs" (render "dev.cljs" data)]
      ["resources/public/index.html" (render "index.html" data)])))

