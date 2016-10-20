export class Message {
  static fromJSON (buffer) {
    return new Message(JSON.parse(buffer.toString()))
  }

  constructor ({ username, command, contents, color = 'black', timestamp }) {
    this.username = username
    this.command = command
    this.contents = contents
    this.color = color
    this.timestamp = timestamp
  }

  toJSON () {
    return JSON.stringify({
      username: this.username,
      command: this.command,
      contents: this.contents,
      color: this.color,
      timestamp: this.timestamp
    })
  }

  toString () {
    const username = this.username
    Object.prototype.toString.call(username)
    const command = this.command
    const timestamp = this.timestamp
    // this.log(`${timestamp}: timestamp`)
    const contents = this.contents
    switch (command) {
      case 'echo':
        return `${timestamp} <${username}> (echo): ${contents}`
      case 'broadcast':
        return `${this.timestamp} <${this.username}> (all): ${contents}`
      case '@':
        return `${timestamp} <${username}> (whisper): ${contents}`
      case 'connect':
        return `${timestamp}: <${username}> has connected`
      case 'disconnect':
        return `${timestamp}: <${username}> has disconnected`
      case 'users':
        return `${timestamp}: currently connected users:\n${JSON.parse(username).join('\n')}`
      default:
        return `${timestamp}: ${contents}`
    }
  }
}
