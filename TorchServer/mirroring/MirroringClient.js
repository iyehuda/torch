let events = require('events');
let util = require('util');
let varint = require('varint');

/**
 * Creates new client that receives varint delimited messages
 * @constructor
 * @extends {events.EventEmitter}
 * @param {net.Socket} socket
 */
function MirroringClient(socket) {
    events.EventEmitter.call(this);
    let self = this;
    /** @type {?number} */
    this.currentLength = null;
    /** @type {?Buffer} */
    this.currentMessage = null;
    /** @type {?number} */
    this.currentOffset = null;
    /** @type {string} */
    this.name = socket.name;
    /** @type {net.Socket} */
    this.socket = socket;
    this.socket.on('data', function (data) {
        self.handleMessage(data);
    });
    this.socket.on('error', function(error) {
        console.error(`${self.toString()} had an error: ${error}`);
    });
    this.socket.on('close', function() {
        console.log(`${this.toString()} disconnected`);
        self.emit('close');
    });
}

util.inherits(MirroringClient, events.EventEmitter);

/**
 * Returns the client
 * @returns {string}
 */
MirroringClient.prototype.toString = function toString() {
    return this.name || 'unknown client';
};

/**
 * Buffers received data and tries to assemble a message
 * @param {Buffer} data
 */
MirroringClient.prototype.handleMessage = function handleMessage(data) {
    if(this.currentLength === null) {
        if(this.currentMessage === null) {
            try {
                this.currentLength = varint.decode(data);
                this.currentOffset = 0;
                data = data.slice(varint.decode.bytes);
                this.handleMessage(data);
            } catch (exception) {
                console.error(exception);
                this.currentMessage = data;
            }
        } else {
            data = Buffer.concat([this.currentMessage, data]);
            this.currentMessage = null;
            this.handleMessage(data);
        }
    } else {
        if(this.currentMessage === null) {
            this.currentMessage = Buffer.alloc(this.currentLength);
            this.currentOffset = 0;
        }

        let dataOffset = data.copy(this.currentMessage, this.currentOffset);
        this.currentOffset += dataOffset;

        if(this.currentOffset === this.currentLength) {
            this.emit('message', this.currentMessage);
            this.currentLength = null;
            this.currentOffset = null;
            this.currentMessage = null;
            if(dataOffset < data.length) {
                data = data.slice(dataOffset);
                this.handleMessage(data);
            }
        }
    }
};

/**
 * Sends a message to the client
 * @param {Buffer} message
 */
MirroringClient.prototype.send = function send(message) {
    this.socket.write(Buffer.from(varint.encode(message.length)));
    this.socket.write(message);
};

/**
 * Closes the connection with the client
 */
MirroringClient.prototype.destroy = function destroy() {
    this.currentLength = null;
    this.currentOffset = null;
    this.currentMessage = null;
    if(this.socket !== null) {
        if(!this.socket.destroyed) {
            this.socket.destroy();
        }
        this.socket = null;
    }
};

module.exports = MirroringClient;
