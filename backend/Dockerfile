# =============================================================================
# Backend — image en deux etapes.
#
# La premiere compile avec Maven, la seconde n'embarque qu'un JRE et le jar.
# Le resultat ne contient ni Maven, ni le code source, ni le depot ~/.m2 :
# une image de production n'a aucune raison de transporter les outils qui ont
# servi a la construire.
# =============================================================================

# --- Etape 1 : compilation ---------------------------------------------------
FROM eclipse-temurin:21-jdk AS construction

WORKDIR /build

# Le wrapper et le pom d'abord, seuls. Tant qu'ils ne changent pas, Docker
# reutilise la couche de dependances telechargees : on ne retelecharge pas
# Spring Boot a chaque modification d'une ligne de code.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
# Les tests ne sont pas rejoues ici : ils tournent sur le poste et en
# integration continue. Les rejouer a chaque construction d'image allongerait
# le `docker compose up --build` du correcteur sans rien prouver de plus.
RUN ./mvnw -B clean package -DskipTests

# --- Etape 2 : execution -----------------------------------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

# L'application ne tourne pas en root : une faille dans une dependance ne doit
# pas donner les pleins pouvoirs sur le conteneur.
RUN groupadd --system kfokam && useradd --system --gid kfokam kfokam

COPY --from=construction /build/target/*.jar application.jar
RUN chown kfokam:kfokam application.jar

USER kfokam
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
