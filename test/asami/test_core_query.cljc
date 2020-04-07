(ns asami.test-core-query
  "Tests the public query functionality"
  (:require [naga.store :refer [new-node assert-data]]
            [asami.core :refer [empty-store q]]
            [schema.core :as s]
            #?(:clj  [clojure.test :refer [is use-fixtures testing]]
               :cljs [clojure.test :refer-macros [is run-tests use-fixtures testing]])
            #?(:clj  [schema.test :as st :refer [deftest]]
               :cljs [schema.test :as st :refer-macros [deftest]]))
  #?(:clj (:import [clojure.lang ExceptionInfo])))

(defn nn [] (new-node empty-store))

(let [pmc (nn)
      gh (nn)
      jl (nn)
      r1 (nn)
      r2 (nn)
      r3 (nn)
      r4 (nn)
      r5 (nn)

      data [[pmc :artist/name "Paul McCartney"]
            [gh :artist/name "George Harrison"]
            [jl :artist/name "John Lennon"]
            [r1 :release/artists pmc]
            [r1 :release/name "My Sweet Lord"]
            [r2 :release/artists gh]
            [r2 :release/name "Electronic Sound"]
            [r3 :release/artists gh]
            [r3 :release/name "Give Me Love (Give Me Peace on Earth)"]
            [r4 :release/artists gh]
            [r4 :release/name "All Things Must Pass"]
            [r5 :release/artists jl]
            [r5 :release/name "Imagine"]]

      store (-> empty-store
                (assert-data data))]

  (deftest test-simple-query
    (let [results (q '[:find ?e ?a ?v
                       :where [?e ?a ?v]] store)
          artists (q '[:find ?artist
                       :where [?e :artist/name ?artist]] store)]
      (is (= (set data) (set results)))
      (is (= #{["Paul McCartney"] ["George Harrison"] ["John Lennon"]} (set artists)))))

  (deftest test-join-query
    (let [results (q '[:find ?name
                       :where [?r :release/name "My Sweet Lord"]
                       [?r :release/artists ?a]
                       [?a :artist/name ?name]] store)]
      (is (= [["Paul McCartney"]] results))))

  (deftest test-join-multi-query
    (let [results (q '[:find ?rname
                       :where [?a :artist/name "George Harrison"]
                       [?r :release/artists ?a]
                       [?r :release/name ?rname]] store)]
      (is (= #{["Electronic Sound"]
               ["Give Me Love (Give Me Peace on Earth)"]
               ["All Things Must Pass"]} (set results)))))

  (deftest test-not-query
    (let [results (q '[:find ?rname
                       :where [?a :artist/name "George Harrison"]
                       [?r :release/artists ?a]
                       [?r :release/name ?rname]
                       (not [?r :release/name "Electronic Sound"])] store)]
      (is (= #{["Give Me Love (Give Me Peace on Earth)"]
               ["All Things Must Pass"]} (set results))))

    (let [results (q '[:find ?rname
                       :where [?a :artist/name "George Harrison"]
                       [?r :release/artists ?a]
                       [?r :release/name ?rname]
                       (not [?ra :release/artists ?aa]
                            [?aa :artist/name "John Lennon"])] store)]
      (is (empty? results)))

    (let [results (q '[:find ?rname
                       :where [?a :artist/name "George Harrison"]
                       [?r :release/artists ?a]
                       [?r :release/name ?rname]
                       (not [?r :release/artists ?aa] [?aa :artist/name "Julian Lennon"])]
                     store)]
      (is (= #{["Electronic Sound"]
               ["Give Me Love (Give Me Peace on Earth)"]
               ["All Things Must Pass"]} (set results)))))

  (deftest test-bindings-query
    (let [all-results (q '[:find ?release-name
                           :where [?artist :artist/name ?artist-name]
                           [?release :release/artists ?artist]
                           [?release :release/name ?release-name]]
                         store)
          results (q '[:find ?release-name
                       :in $ [?artist-name ...]
                       :where [?artist :artist/name ?artist-name]
                       [?release :release/artists ?artist]
                       [?release :release/name ?release-name]]
                     store ["Paul McCartney" "George Harrison"])]
      (is (= #{["My Sweet Lord"] 
               ["Electronic Sound"]
               ["Give Me Love (Give Me Peace on Earth)"] 
               ["All Things Must Pass"]
               ["Imagine"]}
             (set all-results)))
      (is (= #{["My Sweet Lord"] 
               ["Electronic Sound"]
               ["Give Me Love (Give Me Peace on Earth)"] 
               ["All Things Must Pass"]}
             (set results)))))


  (deftest test-query-error
    (is (thrown-with-msg? ExceptionInfo #"Missing ':find' clause"
                          (q '[:select ?e ?a ?v
                               :where [?e ?a ?v]] store)))
    (is (thrown-with-msg? ExceptionInfo #"Missing ':where' clause"
                          (q '[:find ?e ?a ?v
                               :given [?e ?a ?v]] store)))
    (is (thrown-with-msg? ExceptionInfo #"Unknown clauses: "
                          (q '[:select ?e ?a ?v
                               :having [(?v > 5)]
                               :where [?e ?a ?v]] store)))))

(comment "The next structure represents the following data"
  {:disposition 5
   :observable {:type "domain"
                :value "cisco.com"
                :deliberated true}}
  {:disposition 5
   :observable {:type "ip"
                :value "72.163.4.161"
                :deliberated false}}
  {:disposition 1
   :observable {:type "domain"
                :value "ipo.pl"
                :deliberated true
                :internal true}}
  {:disposition 1
   :observable {:type "domain"
                :value "cisco.com"
                :deliberated true}}
  {:disposition 5
   :observable {:type "ip"
                :value "72.163.4.177"
                :deliberated true}})


(let [obs1 (nn)
      obs2 (nn)
      obs3 (nn)
      obs4 (nn)
      obs5 (nn)
      v1 (nn)
      v2 (nn)
      v3 (nn)
      v4 (nn)
      v5 (nn)

      idata [[obs1 :type "domain"]
             [obs1 :value "cisco.com"]
             [obs1 :deliberated true]
             [obs2 :type "ip"]
             [obs2 :value "72.163.4.161"]
             [obs2 :deliberated false]
             [obs3 :type "domain"]
             [obs3 :value "ipo.pl"]
             [obs3 :deliberated true]
             [obs3 :internal true]
             [obs4 :type "domain"]
             [obs4 :value "cisco.com"]
             [obs4 :deliberated true]
             [obs5 :type "ip"]
             [obs5 :value "72.163.4.177"]
             [obs5 :deliberated true]
             [v1 :disposition 5]
             [v1 :observable obs1]
             [v2 :disposition 5]
             [v2 :observable obs2]
             [v3 :disposition 1]
             [v3 :observable obs3]
             [v4 :disposition 1]
             [v4 :observable obs4]
             [v5 :disposition 5]
             [v5 :observable obs5]]]

  (deftest test-negation-query
    (let [st (-> empty-store (assert-data idata))
          observables (q '[:find ?type ?value
                           :where [?observable :type ?type]
                           [?observable :value ?value]
                           [?observable :deliberated true]]
                         st)
          int-observables (q '[:find ?type ?value
                               :where [?observable :type ?type]
                               [?observable :value ?value]
                               [?observable :deliberated true]
                               (not [?observable :internal true])]
                             st)
          disp-observables (q '[:find ?type ?value
                                :where [?observable :type ?type]
                                [?observable :value ?value]
                                [?observable :deliberated true]
                                (not [?observable :internal true])
                                (not [?verdict :disposition 1]
                                     [?verdict :observable ?verdict-observable]
                                     [?verdict-observable :type ?type]
                                     [?verdict-observable :value ?value])] 
                              st)]
      (is (= #{["domain" "cisco.com"]
               ["domain" "ipo.pl"]
               ["ip" "72.163.4.177"]}
             (set observables)))
      (is (= #{["domain" "cisco.com"]
               ["ip" "72.163.4.177"]}
             (set int-observables)))
      (is (= [["ip" "72.163.4.177"]]
             disp-observables))))

  (deftest test-negation-binding-query
    (let [st (-> empty-store (assert-data idata))
          observables (q '[:find ?type ?value
                           :in $ ?observable-type
                           :where [?observable :type ?type]
                           [?observable :value ?value]
                           [?observable :deliberated true]
                           (not [?observable :internal true])
                           (not [?verdict :disposition ?observable-type]
                                [?verdict :observable ?verdict-observable]
                                [?verdict-observable :type ?type]
                                [?verdict-observable :value ?value])] 
                         st 1)]
      (is (= [["ip" "72.163.4.177"]]
             observables)))))

(let [o1 (nn)
      o2 (nn)
      o3 (nn)
      ver1 (nn)
      sight1 (nn)
      other1 (nn)

      ddata [[o1 :type "domain"]
             [o1 :value "cisco.com"]
             [o1 :verdict ver1]
             [o2 :type "ip"]
             [o2 :value "72.163.4.161"]
             [o2 :sighting sight1]
             [o3 :type "domain"]
             [o3 :value "ilo.pl"]
             [o3 :other other1]
             [ver1 :id "verdict-1"]
             [sight1 :id "sighting-1"]
             [other1 :id "other-1"]]]

  (deftest test-disjunctions
    (let [st (-> empty-store (assert-data ddata))
          r1 (q '[:find ?related ?type ?value
                  :where [?observable :value ?value]
                  [?observable :type ?type]
                  (or [?observable :verdict ?related]
                      [?observable :sighting ?related])]
                st)
          r2 (q '[:find ?observable ?related
                  :where
                  (or [?observable :verdict ?related]
                      [?observable :sighting ?related])]
                st)
          r3 (q '[:find ?id ?type ?value
                  :where [?observable :value ?value]
                  [?observable :type ?type]
                  (or [?observable :verdict ?related]
                      [?observable :sighting ?related])
                  [?related :id ?id]]
                st)]
      (is (= #{[ver1 "domain" "cisco.com"]
               [sight1 "ip" "72.163.4.161"]}
             (set r1)))
      (is (= #{[o1 ver1]
               [o2 sight1]}
             (set r2)))
      (is (= #{["verdict-1" "domain" "cisco.com"]
               ["sighting-1" "ip" "72.163.4.161"]}
             (set r3))))))

#?(:cljs (run-tests))

