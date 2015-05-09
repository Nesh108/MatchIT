require 'base64'
require 'excon'
require 'json'

module Sphereio

  def self.login
    client_id = 'cwAObk-HzX0QBORkNlFonCN5'
    client_secret = '4GsMnvKx0H594cDxoWvUFc5npFMes6V7'
    project_key = 'matchit-15'
    encoded = Base64.urlsafe_encode64 "#{client_id}:#{client_secret}"
    headers = { 'Authorization' => "Basic #{encoded}", 'Content-Type' => 'application/x-www-form-urlencoded' }
    body = "grant_type=client_credentials&scope=manage_project:#{project_key}"
    res = Excon.post 'https://auth.sphere.io/oauth/token', :headers => headers, :body => body
    raise "Problems on getting access token from auth.sphere.io: #{res.body}" unless res.status == 200
    JSON.parse(res.body)['access_token']
  end

end