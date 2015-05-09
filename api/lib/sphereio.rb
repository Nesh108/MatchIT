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

  def self.products
    token = Sphereio.login
    project_key = 'matchit-15'
    headers = { 'Authorization' => "Bearer #{token}" }
    res = Excon.get "https://api.sphere.io/#{project_key}/product-projections?limit=100&offset=0", :headers => headers
    JSON.parse res.body
  end

  def self.product_with_matching_color(original_color)
    products_by_color = Sphereio.products['results'].collect do |product|
      next if product['masterVariant']['attributes'].empty?
      product_color = product['masterVariant']['attributes'].select do |attribute|
        attribute['value'] if attribute['name']== 'color'
      end.first
      next if product_color.nil?
      {
          "#{product_color['value']}" => {
              color: product_color['value'],
              id: product['id'],
              name: product['name'],
              price: product['masterVariant']['prices'].first['value'],
              image: product['masterVariant']['images'].first['url']
          }
      }
    end.compact
    products_by_color.collect{|x| Color::RGB.from_html(x.keys.first)}
    matching_color = Color::RGB.from_html(original_color).closest_match(products_by_color.collect{|x| Color::RGB.from_html(x.keys.first)}, threshold_distance = 1000.0)
    products_by_color.select{|x| x.keys.first.to_s == matching_color.html.upcase}.first[matching_color.html.upcase]
  end
end