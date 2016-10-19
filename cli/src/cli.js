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
let command = ''
// It should be a separate file commands.json with corresponding aliases, colors, and messages.
let commands = [ 'users', 'u', 'echo', 'e', 'disconnect', 'd' ]

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]',
       'Connects a user <username> with server. Default host = localhost, port = 8080')  // don't know how to assign default values
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

    let color = 'blue'
    server.on('data', (buffer) => {
      const command = Message.fromJSON(buffer).command
      switch (command) {
        case 'echo':
          color = 'blue'
          break
        case 'users':
          color = 'green'
          break
        case 'disconnect':
          color = 'red'
          break
        default:
          color = 'blue'
          if (command.charAt(0) === '@') color = 'yellow'
      }
      this.log(cli.chalk[color](Message.fromJSON(buffer).toString())) // prints everything that comes from Server
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const inputWords = words(input)
    // this.log(`You entered: <${inputWords}>`)
    let [ userCommand, ...rest ] = inputWords

// Checking if the user provided command matches known commands:
    if (commands.some(cmd => cmd === userCommand) || input.charAt(0) === '@') {
      command = userCommand  // The command changes
      // this.log(`Command <${command}> recognized`)
    } else {                 // using the previous command
      // this.log(`Command <${userCommand}> not recognized`)
      rest = inputWords
    }
    const contents = rest.join(' ')

// Checking again, which is redundant:
    if (command === 'disconnect' || command === 'd') {                      // alias added
      server.end(new Message({ username, command: 'disconnect' }).toJSON() + '\n')
    } else if (command === 'echo' || command === 'e') {                     // alias added
      server.write(new Message({ username, command: 'echo', contents }).toJSON() + '\n')
    } else if (command === 'users' || command === 'u') {                    // alias added
      server.write(new Message({ username, command: 'users', contents }).toJSON() + '\n')
    } else if (input.charAt(0) === '@') {                     // alias added
      server.write(new Message({ username, command: `@${command}`, contents }).toJSON() + '\n')
    } else {
      this.log(`Command <${command}> was not recognized \n
        please, use one of the following: ${commands}`)
    }

    callback()
  })
