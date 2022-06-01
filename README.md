# jbeap-sso_logout_different_apps
## EAP 7
Script para reproducir un issue al hacer logout usando el SSO en Jboss EAP 7.4

Requerimientos:
- Jboss EAP 7.4
- Maven
- Java 8

Para ejecutarlo se ocupa el script reproducer_logout.sh
> Se puede ejecutar dentro de una carpeta del jboss eap 7.4 o configurando la variable de ambiente EAP_HOME.

> Se pueden ver mas opciones de configuracion dentro del mismo script.


El escenario del error es el siguiente:
- Se tienen dos aplicaciones JSF webapp_a y webapp_b
- Se configura un usuario en el ApplicationRealm.
- Se configura el SSO.
- Se deploya la aplicacion webapp_a en un standalone. (node1-webapp_a)
- Se deploya la aplicacion webapp_b en otro standalone. (node2-webapp_b)


Pasos que realiza la prueba:

1. Se verifica que se solicita el login en node1-webapp_a.
2. Hacemos login en node1-webapp_a.
3. Verificamos que estamos logueados en node1-webapp_a.
4. Verificamos que estamos logueados en node2-webapp_b gracias al SSO.
5. Cerramos sesion en node2-webapp_b.
6. Se verifica que se solicita el login en node2-webapp_b.
7. Se verifica que se solicita el login en node1-webapp_a.
8. Se valida que no este asignado el usuario el valor del usuario en el login de node1-webapp_a. Este valor se asigna en el paso 2 al bean de sesion SeguridadMB.

## EAP 6
Branch: eap6

Script para reproducir en el eap 6 donde este problema no ocurre.


