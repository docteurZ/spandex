(ns qbits.spandex.spec
  (:require
   [qbits.spandex]
   [clojure.spec :as s]
   [clojure.core.async :as async]
   [qbits.spandex.utils])
  (:import
   (org.elasticsearch.client
    RestClient
    RestClient$FailureListener)))

(defn chan? [x]
  (instance? clojure.core.async.impl.channels.ManyToManyChannel x))

(s/def ::client #(instance? RestClient %))

(alias 'client-options (create-ns 'qbits.spandex.spec.client-options))
(s/def ::client-options (s/keys :opt-un [::client-options/max-retry-timeout
                                         ::client-options/default-headers
                                         ::client-options/http-client-config-callback
                                         ::client-options/request-config-callback
                                         ::client-options/failure-listener]))

(s/def ::client-options/failure-listener #(instance? RestClient$FailureListener %))
(s/def ::client-options/max-retry-timeout int?)
(s/def ::client-options/request-config-callback fn?)
(s/def ::client-options/http-client-config-callback fn?)
(s/def ::client-options/default-headers (s/map-of (s/or :kw keyword? :str string?) string?))

(alias 'sniffer-options (create-ns 'qbits.spandex.spec.sniffer-options))
(s/def ::sniffer-options (s/keys :opt-un [::sniffer-options/sniff-interval
                                          ::sniffer-options/sniff-after-failure-delay]))
(s/def ::sniffer-options/sniff-interval int?)
(s/def ::sniffer-options/sniff-after-failure-delay int?)

(alias 'request (create-ns 'qbits.spandex.spec.request))
(s/def ::request (s/keys :req-un [::request/url]
                         :opt-un [::request/scheme
                                  ::request/method
                                  ::request/headers
                                  ::request/query-string
                                  ::request/body]))

(s/def ::request/url string?)
(s/def ::request/scheme #{:http :https})
(s/def ::request/method #{:get :post :put :head})
(s/def ::request/headers (s/map-of (s/or :kw keyword? :str string?)
                                   string?))
(s/def ::request/query-string (s/map-of (s/or :kw keyword? :str string?)
                                        any?))

(s/def ::request/body (s/or :str string?
                            :raw #(instance? qbits.spandex.Raw %)
                            :stream #(instance? java.io.InputStream %)
                            :edn any?))

(alias 'response (create-ns 'qbits.spandex.spec.response))
(s/def ::response (s/keys :req-un [::response/body
                                   ::response/status
                                   ::response/headers
                                   ::response/host]))

(s/def ::response/headers (s/map-of string? string?))
(s/def ::response/status pos-int?)
(s/def ::response/body (s/nilable any?)) ;; edn/clj?
(s/def ::response/host any?) ;; to be refine

(s/def ::request-async
  (s/merge ::request
           (s/keys :opt-un [::success ::error ::response-consumer-factory])))

(s/def ::success fn?) ;; refine
(s/def ::error fn?) ;; refine

(s/fdef qbits.spandex/request
        :args (s/cat :client ::client
                     :options ::request)
        :ret ::response)

(s/fdef qbits.spandex/request-async
        :args (s/cat :client ::client
                     :options ::request-async))

(s/fdef qbits.spandex/request-chan
        :args (s/cat :client ::client
                     :options ::request-async)
        :ret chan?)

(s/fdef qbits.spandex/chunks->body
        :args (s/cat :fragments (s/coll-of map?))
        :ret string?)

(s/def ::ttl int?)
(s/def ::ch int?)
(s/fdef qbits.spandex/scroll-chan
        :args (s/cat :client ::client
                     :options (s/and ::request (s/keys :opt-un [::ttl ::ch])))
        :ret chan?)


(s/def ::input-ch chan?)
(s/def ::output-ch chan?)
(s/def ::request-ch chan?)
(s/def ::flush-interval pos-int?)
(s/def ::flush-threshold pos-int?)
(s/def ::max-concurrent-requests pos-int?)
(s/def ::bulk-chan-options (s/and ::request
                                  (s/keys :opt-un [::input-ch
                                                   ::output-ch
                                                   ::flush-threshold
                                                   ::flush-interval
                                                   ::max-concurrent-requests])))
(s/fdef qbits.spandex/bulk-chan
        :args (s/cat :client ::client
                     :options ::bulk-chan-options)
        :ret (s/keys :req-un [::input-ch ::output-ch ::request-ch]))

;; utils
(alias 'utils (create-ns 'qbits.spandex.spec.utils))
(s/fdef qbits.spandex.utils/url
        :args (s/cat :parts (s/* #(satisfies? qbits.spandex.utils/URLFragment %)))
        :ret string?)
