(ns cassiel.cubewar.tools)

(defn check-against
  "Check an `ExceptionInfo` against a `:type` and a filter.
   Midje and Slingshot really need to get their shit together."
  [type filter exn]
  (let [obj (-> exn (.getData) (:object))]
    (and (= (:type obj) type)
         (filter obj))))
