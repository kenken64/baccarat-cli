SRC_DIR = src
OUT_DIR = classes
PORT_NO=3000
HOSTNAME=localhost
DECK_OF_CARDS=4
WORKSHOP_JAR = bacarrat-cli.jar

SOURCES = $(shell find $(SRC_DIR) -name '*.java')
JAVAC = javac
JFLAGS = -d $(OUT_DIR) -cp $(OUT_DIR)
JAVA = java

CLIENT_APP = client.ClientApp
SERVER_APP = server.ServerApp

all: ${OUT_DIR} compile

${OUT_DIR}:
	mkdir -p ${OUT_DIR}

compile: ${SOURCES}
	${JAVAC} ${} ${JFLAGS} ${SOURCES}

run-client: compile
	${JAVA} -cp ${OUT_DIR} ${CLIENT_APP} ${HOSTNAME}:${PORT_NO}  

run-server: compile
	${JAVA} -cp ${OUT_DIR} ${SERVER_APP} ${PORT_NO} ${DECK_OF_CARDS}

jar: compile
	jar cvf ${WORKSHOP_JAR} -C ${OUT_DIR} .

run-client-jar: jar
	${JAVA} -cp ${WORKSHOP_JAR} ${CLIENT_APP} ${HOSTNAME}:${PORT_NO}  

run-server-jar: jar
	${JAVA} -cp ${WORKSHOP_JAR} ${SERVER_APP} ${PORT_NO} ${DECK_OF_CARDS}

clean:
	@rm -rf ${OUT_DIR}
	@rm -rf ${WORKSHOP_JAR}
	
