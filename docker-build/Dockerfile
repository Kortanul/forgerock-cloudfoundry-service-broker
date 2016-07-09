FROM ubuntu
RUN apt-get update && apt-get install -y \
  curl \
  git \
  python \
  python-pip \
  ruby
RUN pip install --upgrade pip
RUN gem install bosh_cli --no-ri --no-rdoc

ADD app /app

CMD ["/bin/bash", "/app/build-tile.sh"]
