Autor: Grupo D - Dandan Liang 

Objetivo: El objetivo del plugin es utilizar tsDetect para detectar los posibles test smells presentes en los códigos en la fase de test.

Descripción de la funcionalidad:
Una vez que el plugin se ejecuta, realiza los siguientes pasos:
1. Identificar automáticamente los archivos del proyecto y generar con esta información un documento .csv dentro de la carpeta target.
2. En caso de que no existe, descargar automáticamente tsDetect.jar desde Github y lo guarda en la carpeta target.
3. El archivo generado en el paso 1, se pasará a tsDetect como input, y leerá los resultados producidos por este.
4. Los resultados de la detección generan las siguientes salidas:
    - Un archivo CSV con el nombre estilo Output_TestSmellDetection_XXXX, de formato .csv guardado en la carpeta target.
    - Un resumen mostrado en el terminal, listando los problemas detectados estilo:
        [INFO] ------------------------------------------------------------------------
        [INFO] Test Smells Detection Plugin (tsDetect)
        [INFO] ------------------------------------------------------------------------
        [INFO] Downloading TestSmellDetector jar...
        [INFO] Found TestSmellDetecto output: Output_TestSmellDetection_1765926248320.csv
        [INFO] ------------------------------------------------------------------------
        [INFO] Test Smells Result
        [INFO] ------------------------------------------------------------------------
        [INFO] Assertion Roulette: 2
        [INFO] Conditional Test Logic: 1
        [INFO] Eager Test: 2
        [INFO] Lazy Test: 6
        [INFO] Magic Number Test: 5
        [INFO] Total: 16
        [INFO] ------------------------------------------------------------------------
        [INFO] BUILD SUCCESS
        [INFO] ------------------------------------------------------------------------
        [INFO] Total time:  1.723 s
        [INFO] Finished at: 2025-12-17T00:04:08+01:00
        [INFO] ------------------------------------------------------------------------

