# Progetto di laboratorio di Programmazione III

Si sviluppi un’applicazione Java che implementi un servizio di posta elettronica organizzato con un mail server 
che gestisce le caselle di posta elettronica degli utenti e i mail client necessari per permettere agli utenti di accedere alle proprie caselle di posta.

### Mail server

* Il mail server gestisce una lista di caselle di posta elettronica e ne mantiene la persistenza utilizzando file (txt o binari, a vostra scelta, non si possono usare database) per memorizzare i messaggi in modo permanente.
* Il mail server ha un’interfaccia grafica sulla quale viene visualizzato il log delle azioni effettuate dai mail clients e degli eventi che occorrono durante l’interazione tra i client e il server.
* Una casella di posta elettronica contiene:
  * Nome dell’account di mail associato alla casella postale.
  * Lista (eventualmente vuota) di messaggi. I messaggi di posta elettronica sono istanze di una classe Email che specifica ID, mittente, destinatario/i, argomento, testo e data di spedizione del messaggio.

### Mail client

* Il mail client, associato a un particolare account di posta elettronica, ha un’interfaccia grafica così caratterizzata:
  * L’interfaccia permette di:
    * creare e inviare un messaggio a uno o più destinatari (destinatari multipli di un solo messaggio di posta elettronica)
    * leggere i messaggi della casella di posta
    * rispondere a un messaggio ricevuto, in Reply (al mittente del messaggio) e/o in Reply-all (al mittente e a tutti i destinatari del messaggio ricevuto)
    * girare (forward) un messaggio a uno o più account di posta elettronica
    * rimuovere un messaggio dalla casella di posta.
  * L’interfaccia mostra sempre la lista aggiornata dei messaggi in casella e, quando arriva un nuovo messaggio, notifica l’utente attraverso una finestra di dialogo.
* Il mail client non deve andare in crash se il mail server viene spento – gestire i problemi di connessione al mail server inviando opportuni messaggi di errore all’utente e fare in modo che il mail client si riconnetta automaticamente al server quando questo è novamente attivo.

### Requisiti tecnici

* Per la dimostrazione si assuma di avere 3 utenti di posta elettronica che comunicano tra loro. Si progetti però il sistema in modo da renderlo scalabile a molti utenti.
* L’applicazione deve essere sviluppata in Java (JavaFXML) e basata su architettura MVC, con Controller + viste e Model, seguendo i principi del pattern Observer Observable. Si noti che non deve esserci comunicazione diretta tra viste e model: ogni tipo di comunicazione tra questi due livelli deve essere mediato dal controller o supportata dal pattern Observer Observable. Non si usino le classi deprecate Observer.java e Observable.java. Si usino le classi di JavaFX che supportano il pattern Observer Observable.
* L’applicazione deve permettere all’utente di correggere eventuali input errati (per es., in caso di inserimento di indirizzi di posta elettronica non esistenti, il server deve inviare messaggio di errore al client che ha inviato il messaggio; inoltre, in caso di inserimento di indirizzi sintatticamente errati il client stesso deve segnalare il problema senza tentare di inviare i messaggi al server).
* I client e il server dell’applicazione devono parallelizzare le attività che non necessitano di esecuzione sequenziale e gestire gli eventuali problemi di accesso a risorse in mutua esclusione. NB: i client e il server di mail devono essere applicazioni java distinte; la creazione/gestione dei messaggi deve avvenire in parallelo alla ricezione di altri messaggi.
* L’applicazione deve essere distribuita (i mail client e il server devono stare tutti su JVM distinte) attraverso l’uso di Socket Java.
* Non gestite socket permanenti per collegare client e server: fate in modo che, come HTTP, il client chieda di aprire la connessione ogni volta che ha bisogno di fare un'operazione. Inoltre, non fate che ad ogni richiesta di aggiornamento dei messaggi di un client il server scarichi sul client l'intera mailbox: il server deve solo inviare i messaggi che non sono stati precedentemente distribuiti al client.

### Requisiti dell'interfaccia utente

L’interfaccia utente deve essere:

* Comprensibile (trasparenza). In particolare, a fronte di errori, deve segnalare il problema all’utente.
* Ragionevolmente efficiente (funzionale) per permettere all’utente di eseguire le operazioni con un numero minimo di click e di inserimenti di dati.
* Deve essere implementata utilizzando JavaFXML e, se necessario, Thread java. Non è richiesto, ma consigliato, l’uso di Java Beans, properties e binding di properties.

### Note

Si raccomanda di prestare molta attenzione alla progettazione dell’applicazione per facilitare il parallelismo nell’esecuzione delle istruzioni e la distribuzione su JVM diverse.
