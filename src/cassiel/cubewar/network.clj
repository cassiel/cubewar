(ns cassiel.cubewar.network
  "OSC networking utilities."
  (:import [java.net InetAddress]
           [net.loadbang.osc.comms UDPTransmitter UDPReceiver]
           [net.loadbang.osc.data Message]
           [net.loadbang.osc.exn CommsException]))

(defn unpack-message
  "Unpack a message and call `f` with address and list of args."
  [f ^Message m]
  (let [address (get (re-find #"^/?(.+)+$" (.getAddress m)) 1)
        args (for
                 [i (range (.getNumArguments m))]
               (.getValue (.getArgument m i)))]
    (f (keyword address) args)))

(defn start-receiver
  "Create a receiver socket given a consuming function. The receiver accepts `(.stop)`."
  [port f]
  (let [rx (proxy
               [UDPReceiver]
               [port]
             (consumeMessage [_date00 m] (unpack-message f m)))
        _ (.open rx)]

    (.start (Thread. (reify Runnable
                       (run [this]
                         (try
                           (dorun (repeatedly #(.take rx)))
                           (catch CommsException _ nil))))))
    rx))

(defn start-transmitter
  "Create a transmitter to host and port. The transmitter accepts `(.close)`."
  [host port]
  (UDPTransmitter. (InetAddress/getByName host) port))
