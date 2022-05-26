# jbeap-sso_logout_different_apps
Script para reproducir un issue al hacer logout usando el SSO en Jboss EAP 7.4

Para ejecutarlo se ocupa el script reproducer_logout.sh

El escenario del error es el siguiente:
Se tienen dos aplicaciones JSF webapp_a y webapp_b
Se configura el SSO.
Se deploya la aplicacion webapp_a en un standalone. (node1-webapp_a)
Se deploya la aplicacion webapp_b en otro standalone. (node2-webapp_b)

Pasos que realiza la prueba:
1.-Se verifica que se solicita el login en node1-webapp_a.
2.-Hacemos login en node1-webapp_a.
3.-Verificamos que estamos logueados en node1-webapp_a.
4.-Se obtiene el session id de node1-webapp_a.
5.-Verificamos que estamos logueados en node2-webapp_b gracias al SSO.
6.-Cerramos sesion en node2-webapp_b.
7.-Se verifica que se solicita el login en node2-webapp_b.
8.-Se verifica que se solicita el login en node1-webapp_a.
9.-Se obtiene el session id de node1-webapp_a.
10.-Se valida que el session id del paso 4 sea diferente al session id del paso 9. 
Se supone que al cerra la sesion deberia cambiar el session id. Y es este punto donde falla porque ambos session id son iguales.
