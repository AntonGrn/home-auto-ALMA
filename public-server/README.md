# PublicServer

ALMA PublicServer is a service running on a public network. Its purpose is to connect remote AndroidClients to HomeServer instances, without the need to port-forward HomeServers and by providing necessary security for the communicaiton. 

See [main page](https://github.com/AntonGrn/home-auto-ALMA-v2.0#publicserver-remotely-accessible) for more information about the PublicServer.

## Database tables
Listed below are the database tables required to run ALMA PublicServer.

## ALMA web admin tool
* Web interface with login feature for admin use.
* Track connection activity on the ALMA PublicServer.
  * Logs both successful and unsuccesful login attempts.
  * Links repeated server visitors.
* Usage:
  * Click any entity to search and display matches in the server history.
  * Green rows: Successful logins.
  * Red rows: Unsuccessful logins and the cause:
    * *Invalid cryptography*: Client does not conform to the cryptography scheme of distributing secure keys.
    * *Invalid connection format*: Client does not conform to the ALMA communication protocol.
    * *Unsuccessful login attempt*: Client has entered invalid login credentials.

<img src="./images/web_admin_activity.png">
