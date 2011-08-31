package mosaic.cloudlet.tests;

public class PingMessage {

		private String key;
		
		public PingMessage(){
			
		}
		
		public PingMessage(String key) {
			this.key=key;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}
}
