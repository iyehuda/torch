let events = require('events');
let util = require('util');

/**
 * Creates a new empty session
 * @param {number} id
 * @constructor
 * @extends events.EventEmitter
 */
function MirroringSession(id) {
    events.EventEmitter.call(this);
    /**
     * The session id
     * @type {number}
     */
    this.id= id;
    /**
     * Specifies whether the session is destroyed
     * @type {boolean}
     */
    this.destroyed = false;
    /**
     * The receiver client of the session, connects first
     * @type {MirroringClient}
     */
    this.receiver = null;
    /**
     * The sender client of the session, connects second
     * @type {MirroringClient}
     */
    this.sender = null;
}

util.inherits(MirroringSession, events.EventEmitter);

/**
 * Checks whether the session contains sender and receiver
 * @returns {boolean}
 */
MirroringSession.prototype.isFull = function isFull() {
    return this.receiver !== null && this.sender !== null;
};

/**
 * Adds a client to the session, the first is the receiver, the second is the sender
 * If the session is already full, false will be returned, true otherwise
 * @param {MirroringClient} client
 * @returns {boolean}
 */
MirroringSession.prototype.addClient = function addClient(client) {
    let self = this;
    if(this.receiver === null)
        this.receiver = client;
    else if(this.sender === null) {
        this.sender = client;
        this.sender.on('message', function (message) {
            console.log(`forwarding ${message.length} bytes`);
            self.receiver.send(message);
        });
    } else {
        console.warn('tried to add a client to full session');
        return false;
    }
    client.on('close', function () {
        self.destroy();
    });
    return true;
};

/**
 * Disconnects the session participators
 */
MirroringSession.prototype.destroy = function destroy() {
    if(this.destroyed)
        return;
    this.destroyed = true;
    if(this.receiver !== null) {
        this.receiver.destroy();
        this.receiver = null;
    }
    if(this.sender !== null) {
        this.sender.destroy();
        this.sender = null;
    }

    this.emit('close');
};

module.exports = MirroringSession;
