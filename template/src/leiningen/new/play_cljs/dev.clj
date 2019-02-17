(require
  '[figwheel.main :as figwheel]
  '[nightlight.core :as nightlight])

(nightlight/start {:port 4000 :url "http://localhost:9500"})
(figwheel/-main "--build" "dev")
