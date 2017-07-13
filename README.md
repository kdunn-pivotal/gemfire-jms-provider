# gemfire-jms-provider

This project demonstrates how to embed the FFMQ JMS provider and a corresponding JMS consumer into GemFire directly.  This turns GemFire into a streaming JMS target for applications like Oracle's Golden Gate via (Application Adapters)[http://www.oracle.com/us/products/middleware/data-integration/goldengate/application-adapters/overview/index.html].

### Current status:
- [X] Embed and test FFMQ JMS provider running _inside_ GemFire
- [ ] Embed and test FFMQ JMS consumer running _inside_ GemFire
- [ ] Re-factor test application to not be a complete hack
- [ ] Test end-to-end workflow via a test JMS producer application running _outside_ GemFire
