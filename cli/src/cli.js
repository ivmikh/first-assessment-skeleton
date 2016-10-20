import vorpal from 'vorpal'
// import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server

const hostDefault = 'localhost'
const portDefault = 8080
let host
let port

// It might should be a separate file of commands with corresponding aliases, colors, and messages.
let commands = [ 'users', 'u', 'echo', 'e', 'disconnect', 'd', 'broadcast', 'b' ]

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]',
       'Connects a user <username> with server. Default host = localhost, port = 8080')
  .alias('c', 'co', 'con', 'conn', 'conne', 'connec')                                   // alias added
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    host = args.host ? args.host : hostDefault
    port = args.port ? args.port : portDefault
    server = connect({ host: host, port: port }, () => {  // what if it cannot connect to a given host/port? (to be done!)
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      const msg = Message.fromJSON(buffer)
      this.log(cli.chalk[msg.color](msg.toString())) // prints everything that comes from Server
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    // const inputWords = words(input)
    const [ command, ...rest ] = input.split(' ')

    const contents = rest.join(' ')

    if (command === '' || command === 'help') {
      this.log(`Possible commands and aliases: ${commands}`)
    } else if (command === 'disconnect' || command === 'd') {                    // alias added
      server.end(new Message({ username, command: 'disconnect' }).toJSON() + '\n')
    } else if (command === 'broadcast' || command === 'b') {                     // alias added
      server.write(new Message({ username, command: 'broadcast', contents }).toJSON() + '\n')
    } else if (command === 'echo' || command === 'e') {                          // alias added
      server.write(new Message({ username, command: 'echo', contents }).toJSON() + '\n')
    } else if (command === 'users' || command === 'u') {                         // alias added
      server.write(new Message({ username, command: 'users', contents }).toJSON() + '\n')
    // } else if (command.charAt(0) === '@') {
    //   server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    }

    callback()
  })
