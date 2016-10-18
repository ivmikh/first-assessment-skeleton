import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server

let hostDefault = 'localhost'
let portDefault = 8080
let host
let port

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]')  // don't know how to assign default values
  .alias('c', 'co', 'con', 'conn', 'conne', 'connec')                                   // alias added
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    host = args.host ? args.host : hostDefault
    port = args.port ? args.port : portDefault
    server = connect({ host: host, port: port }, () => {  // what if it cannot connect to a given host/port? (to be done!)
    // server = connect({ host: 'localhost1', port: 8081 }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString()) // prints everything that comes from Server
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = words(input)
    const contents = rest.join(' ')

    if (command === 'disconnect' || command === 'd') {                      // alias added
      server.end(new Message({ username, command: 'disconnect' }).toJSON() + '\n')
    } else if (command === 'echo' || command === 'e') {                     // alias added
      // `${timestamp} <${username}> (echo): ${contents}`
      server.write(new Message({ username, command: 'echo', contents }).toJSON() + '\n')
    } else if (command === 'users' || command === 'u') {                    // alias added
      server.write(new Message({ username, command: 'users', contents }).toJSON() + '\n')
    } else {
      this.log(`Command <${command}> was not recognized`)
    }

    callback()
  })
